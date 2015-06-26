package nl.wetgos.starthosting.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public interface Command {

    /**
     * Runs this command with the given context.
     *
     * @param context
     * @throws Exception
     */
    void run(CommandContext context) throws Exception;

    /**
     * Configures the possible command line options for this command.
     *
     * @param options
     */
    void configureOptions(Options options);

    /**
     * Gets the description of this command that will be displayed in the usage help.
     *
     * @return
     */
    String getDescription();

}
