package nl.wetgos.starthosting.commands.dynamic;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class URLPublicIPAddressProvider implements PublicIPAddressProvider {

    private final String url;

    private final HttpClient httpClient;

    public URLPublicIPAddressProvider(String url) {
        this.url = url;

        httpClient = HttpClientBuilder.create()
                .disableRedirectHandling()
                .build();
    }

    @Override
    public String getPublicIPAddress() {
        try {
            @Cleanup("releaseConnection")
            HttpGet request = new HttpGet(url);

            log.debug("Requesting {}", url);

            HttpResponse response = httpClient.execute(request);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IllegalStateException("Unexpected response status: " + response.getStatusLine());
            }

            @Cleanup
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            response.getEntity().writeTo(byteArrayOutputStream);

            String content = byteArrayOutputStream.toString("UTF-8").trim();

            log.debug("Server response: {}", content);

            return content;
        } catch (IOException e) {
            throw new RuntimeException("Could not get public IP address from " + url, e);
        }
    }
}
