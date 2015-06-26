package nl.wetgos.starthosting.commands.dynamic;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import nl.wetgos.starthosting.client.DNSRecord;
import nl.wetgos.starthosting.client.StartHostingClient;
import nl.wetgos.starthosting.commands.Command;
import nl.wetgos.starthosting.commands.CommandContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class DynamicDNSCommand implements Command {

    private Properties publicIPAddressURLList;

    @Override
    public void run(CommandContext context) throws Exception {
        CommandLine commandLine = context.getCommandLine();

        String[] urls = commandLine.getOptionValue("url").split(",");
        long interval = Long.parseLong(commandLine.getOptionValue("interval"));
        String[] domains = commandLine.getOptionValue("domain").split(",");
        String type = commandLine.getOptionValue("type");
        String name = commandLine.getOptionValue("name");

        List<PublicIPAddressProvider> providers = Arrays.stream(urls)
                .map(url -> new URLPublicIPAddressProvider(url))
                .collect(Collectors.toList());

        PublicIPAddressWatcher watcher = new PublicIPAddressWatcher(providers, interval);

        watcher.watch(publicIPAddress -> {
            try {
                @Cleanup
                StartHostingClient client = context.getClientFactory().createClient();

                performDynamicUpdate(domains, type, name, publicIPAddress, client);

            } catch (Exception e) {
                log.error("Could not perform dynamic DNS update", e);
            }
        });
    }

    private void performDynamicUpdate(String[] domains, String type, String name, String value, StartHostingClient client) {
        for (String domain : domains) {
            performDynamicUpdate(type, name, value, client, domain);
        }
    }

    private void performDynamicUpdate(String type, String name, String value, StartHostingClient client, String domain) {
        client.changeDomain(domain);

        List<DNSRecord> dnsRecords = client.getDNSRecords();

        dnsRecords.forEach(dnsRecord -> {
            if (type != null && !type.equalsIgnoreCase(dnsRecord.getType())) {
                return;
            }

            if (name != null && !name.equalsIgnoreCase(dnsRecord.getName())) {
                return;
            }

            if (dnsRecord.getId() != null) {
                dnsRecord.setContent(value);

                client.updateDNSRecord(dnsRecord);
            } else {
                log.warn("Ignoring virtual DNS record: {}", dnsRecord);
            }
        });
    }

    @Override
    public void configureOptions(Options options) {
        // daemon options
        options.addOption(Option.builder("interval").hasArg().required().desc("Check interval (milliseconds)").build());

        // public IP options
        options.addOption(Option.builder("url").hasArg().required().desc("Public IP URL(s) to watch (i.e. http://icanhazip.com/)").build());

        // matching options
        options.addOption(Option.builder("domain").hasArg().required().desc("Domain name(s) to update (i.e. mydomain.com)").build());
        options.addOption(Option.builder("type").hasArg().valueSeparator(',').desc("Record type(s) to update (optional)").build());
        options.addOption(Option.builder("name").hasArg().valueSeparator(',').desc("Name(s) to update (optional)").build());
    }

    @Override
    public String getDescription() {
        return "dynamic DNS updater";
    }

}
