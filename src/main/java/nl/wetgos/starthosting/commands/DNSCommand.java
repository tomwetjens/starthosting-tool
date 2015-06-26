package nl.wetgos.starthosting.commands;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import nl.wetgos.starthosting.client.DNSRecord;
import nl.wetgos.starthosting.client.StartHostingClient;
import nl.wetgos.starthosting.commands.Command;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.List;

@Slf4j
public class DNSCommand implements Command {

    @Override
    public void run(CommandContext context) throws Exception {
        CommandLine commandLine = context.getCommandLine();

        String domain = commandLine.getOptionValue("domain");
        String type = commandLine.getOptionValue("type");
        String name = commandLine.getOptionValue("name");
        String value = commandLine.getOptionValue("value");

        @Cleanup
        StartHostingClient client = context.getClientFactory().createClient();

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
        options.addOption(Option.builder("domain").hasArg().required().desc("Domain name").build());
        options.addOption(Option.builder("type").hasArg().desc("Record type").build());
        options.addOption(Option.builder("name").hasArg().desc("Name").build());
        options.addOption(Option.builder("value").hasArg().desc("Value").build());
    }

    @Override
    public String getDescription() {
        return "perform DNS operations";
    }
}
