package nl.wetgos.starthosting.commands;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nl.wetgos.starthosting.client.StartHostingClientFactory;
import org.apache.commons.cli.CommandLine;

@RequiredArgsConstructor
@Getter
public class CommandContext {

    private final CommandLine commandLine;
    private final StartHostingClientFactory clientFactory;

}
