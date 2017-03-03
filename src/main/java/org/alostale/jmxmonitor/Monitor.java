package org.alostale.jmxmonitor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Monitor {

  public static void main(String[] args) throws ParseException {
    CommandLine cli = getCliOptions(args);

    int pid = Integer.parseInt(cli.getOptionValue("pid"));

    System.out.println(pid);
  }

  private static CommandLine getCliOptions(String[] args) {
    Option help = new Option("help", "print this message");

    Options ops = new Options();
    ops.addOption(help);

    DefaultParser parser = new DefaultParser();
    // check help without adding required options
    boolean showHelp = false;
    try {
      showHelp = parser.parse(ops, args).hasOption("help");
    } catch (ParseException ingore) {
    }

    Option pid = Option.builder("p").longOpt("pid").argName("pid").type(Integer.class).hasArg()
        .required().desc("Java process id to monitor").build();

    ops.addOption(pid);

    if (showHelp) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("Monitor", ops);
      System.exit(0);
    }

    try {
      return parser.parse(ops, args);
    } catch (ParseException e) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("Monitor", ops);
      System.exit(1);
      return null;
    }
  }
}
