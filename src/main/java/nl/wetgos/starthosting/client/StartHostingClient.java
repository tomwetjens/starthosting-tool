package nl.wetgos.starthosting.client;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class StartHostingClient implements Closeable {

    public static final String LOCATION_HEADER = "Location";
    private final String baseUrl;

    private final HttpClient httpClient;

    private boolean loggedIn;
    private String activeDomain;

    public StartHostingClient(String baseUrl) {
        this.baseUrl = baseUrl;

        httpClient = HttpClientBuilder.create()
                .disableRedirectHandling()
                .build();
    }

    public void login(String username, String password) throws StartHostingClientException {
        log.info("Logging in as {}", username);

        @Cleanup("releaseConnection")
        HttpPost request = new HttpPost(baseUrl + "/services/logon/");

        List<NameValuePair> params = new LinkedList();

        params.add(new BasicNameValuePair("login", "1"));
        params.add(new BasicNameValuePair("username", username));
        params.add(new BasicNameValuePair("password", password));

        try {
            request.setEntity(new UrlEncodedFormEntity(params));

            HttpResponse response = httpClient.execute(request);

            ensureResponseStatus(response, 302);

            String location = getLocationHeader(response);
            if (!location.contains("/ffstart")) {
                throw new IllegalStateException("Unexpected Location header: " + location);
            }

            log.debug("Successfully logged in as {}", username);

            loggedIn = true;
        } catch (Exception e) {
            throw new StartHostingClientException("Could not login as " + username, e);
        }
    }

    private void ensureResponseStatus(HttpResponse response, int expectedStatusCode) {
        if (response.getStatusLine().getStatusCode() != expectedStatusCode) {
            throw new IllegalStateException("Expected response status " + expectedStatusCode + ", but got: " + response.getStatusLine());
        }
    }

    private String getLocationHeader(HttpResponse response) {
        Header header = response.getFirstHeader(LOCATION_HEADER);

        if (header == null) {
            throw new IllegalStateException("Header not found in response: " + LOCATION_HEADER);
        }

        return header.getValue();
    }

    public void changeDomain(String domain) throws StartHostingClientException {
        ensureLoggedIn();

        if (domain.equalsIgnoreCase(activeDomain)) {
            log.debug("Domain already active: {}", domain);
        }

        log.info("Changing active domain to: {}", domain);

        @Cleanup("releaseConnection")
        HttpGet request = new HttpGet(baseUrl + "/services/domainchanger/?domain=" + domain);

        try {
            HttpResponse response = httpClient.execute(request);

            ensureResponseStatus(response, 302);

            log.debug("Successfully changed active domain to: {}", domain);

            activeDomain = domain;
        } catch (Exception e) {
            throw new StartHostingClientException("Could not change active domain to: " + domain, e);
        }
    }

    private void ensureLoggedIn() {
        if (!loggedIn) {
            throw new StartHostingClientException("Not logged in");
        }
    }

    public List<DNSRecord> getDNSRecords() throws StartHostingClientException {
        ensureDomainActive();

        log.info("Getting DNS records for domain {}", activeDomain);

        @Cleanup("releaseConnection")
        HttpGet request = new HttpGet(baseUrl + "/modules/ffdns/?action=edit");

        try {
            HttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IllegalStateException("Unexpected response status: " + response.getStatusLine());
            }

            InputStream content = response.getEntity().getContent();

            return parseDNSRecords(content);
        } catch (Exception e) {
            throw new StartHostingClientException("Could not get DNS records", e);
        }
    }

    private List<DNSRecord> parseDNSRecords(InputStream content) throws IOException {
        Document document = Jsoup.parse(content, "UTF-8", "");

        Elements rows = document.getElementsByClass("dnsrecord");

        List<DNSRecord> records = new LinkedList();

        for (Element row : rows) {
            String type = row.child(0).ownText().toUpperCase();

            String name = row.child(1).ownText();
            name = name.toLowerCase().replace("." + activeDomain, "");
            name = name.replace(activeDomain, "");

            String ownText = row.child(3).ownText();

            String id = null;

            Elements actions = row.getElementsByTag("a");

            for (Element action : actions) {
                String href = action.attr("href");

                Matcher matcher = Pattern.compile("record=(\\d+)").matcher(href);
                if (matcher.find()) {
                    id = matcher.group(1);
                    break;
                }
            }

            records.add(new DNSRecord(id, type, name, ownText));
        }

        return records;
    }

    private void ensureDomainActive() {
        ensureLoggedIn();

        if (activeDomain == null) {
            throw new StartHostingClientException("No domain active");
        }
    }

    public void updateDNSRecord(DNSRecord dnsRecord) throws StartHostingClientException {
        ensureDomainActive();

        if (dnsRecord == null) {
            throw new IllegalArgumentException("DNS record must not be null");
        }

        if (dnsRecord.getId() == null || "".equals(dnsRecord.getId())) {
            throw new IllegalArgumentException("DNS record must have an ID");
        }

        log.info("Updating DNS record: {}", dnsRecord);

        @Cleanup("releaseConnection")
        HttpPost request = new HttpPost(baseUrl + "/modules/ffdns/?action=edit");

        List<NameValuePair> params = new LinkedList();

        params.add(new BasicNameValuePair("process", "edit_record"));
        params.add(new BasicNameValuePair("record", dnsRecord.getId()));
        params.add(new BasicNameValuePair("type", dnsRecord.getType()));
        params.add(new BasicNameValuePair("name", dnsRecord.getName()));
        params.add(new BasicNameValuePair("content", dnsRecord.getContent()));

        try {
            request.setEntity(new UrlEncodedFormEntity(params));

            HttpResponse response = httpClient.execute(request);

            ensureResponseStatus(response, 200);

            Document document = Jsoup.parse(response.getEntity().getContent(), "UTF-8", "");

            Elements rows = document.getElementsByClass("dnsrecord");

            if (rows.size() == 0) {
                String contentText = document.getElementsByClass("content").text();
                throw new IllegalStateException("Could not update DNS record " + dnsRecord + ": " + contentText);
            }

            log.debug("Successfully updated DNS record {}", dnsRecord.getId());
        } catch (Exception e) {
            throw new StartHostingClientException("Could not update DNS record " + dnsRecord, e);
        }
    }

    @Override
    public void close() throws IOException {

    }
}
