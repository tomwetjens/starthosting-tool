package nl.wetgos.starthosting;

import lombok.extern.slf4j.Slf4j;
import nl.wetgos.starthosting.client.StartHostingClientFactory;
import nl.wetgos.starthosting.commands.Command;
import nl.wetgos.starthosting.commands.CommandContext;
import nl.wetgos.starthosting.commands.DNSCommand;
import nl.wetgos.starthosting.commands.dynamic.DynamicDNSCommand;
import org.apache.commons.cli.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class StartHostingTool {

    private static final String DEFAULT_BASE_URL = "http://hostingmanager.starthosting.nl/server8";

    private final Map<String, Command> commands = new HashMap();

    public StartHostingTool() {
        commands.put("dns", new DNSCommand());
        commands.put("dynamic", new DynamicDNSCommand());
    }

    private void run(String[] args) {
        CommandLineParser commandLineParser = new DefaultParser();

        Options defaultOptions = new Options();

        configureDefaultOptions(defaultOptions);

        CommandLine commandLine;

        try {
            if (args.length == 0) {
                throw new ParseException("command required");
            }

            String commandName = args[0];

            Command command = commands.get(commandName);

            if (command == null) {
                throw new ParseException("command unknown: " + commandName);
            }

            runCommand(args, commandLineParser, defaultOptions, command);
        } catch (ParseException e) {
            System.err.println(e.getMessage());

            StringBuilder header = new StringBuilder();

            header.append("\nAvailable commands:\n");
            for (Map.Entry<String, Command> command : commands.entrySet()) {
                header.append(String.format("    %s%40s\n", command.getKey(), command.getValue().getDescription()));
            }
            header.append("\nOptions:\n");

            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("java -jar " + getClass().getName() + " command", header.toString(), defaultOptions, null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void runCommand(String[] args, CommandLineParser commandLineParser, Options defaultOptions, Command command) throws Exception {
        command.configureOptions(defaultOptions);

        CommandLine commandLine = commandLineParser.parse(defaultOptions, args);

        CommandContext commandContext = createCommandContext(commandLine);

        command.run(commandContext);
    }

    private CommandContext createCommandContext(CommandLine commandLine) {
        StartHostingClientFactory clientFactory = createClientFactory(commandLine);
        return new CommandContext(commandLine, clientFactory);
    }

    private StartHostingClientFactory createClientFactory(CommandLine commandLine) {
        String user = commandLine.getOptionValue("user");
        String password = commandLine.getOptionValue("password");

        return new StartHostingClientFactory(DEFAULT_BASE_URL, user, password);
    }

    private void configureDefaultOptions(Options options) {
        options.addOption(Option.builder("user").desc("User name").hasArg().required().build());
        options.addOption(Option.builder("password").desc("Password").hasArg().required().build());
    }

    public static void main(String[] args) {
        new StartHostingTool().run(args);
    }
}
