package org.alostale.jmxmonitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.alostale.jmxmonitor.writer.CsvWriter;
import org.alostale.jmxmonitor.writer.OutputWriter;
import org.alostale.jmxmonitor.writer.StdinWriter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import sun.tools.jconsole.LocalVirtualMachine;

public class Monitor {
  private int pid = -1;
  private List<JmxAttribute> attributes = new ArrayList<>();
  private long interval;

  private List<OutputWriter> writers = new ArrayList<>(2);

  public static void main(String[] args) throws ParseException {
    Monitor monitor = new Monitor(args);
    monitor.execute();
  }

  public Monitor(String[] args) {
    CommandLine params = getCliOptions(args);
    writers.add(new StdinWriter());
    if (params.hasOption("pid")) {
      pid = Integer.parseInt(params.getOptionValue("pid"));
    }
    if (params.hasOption("memory")) {
      try {
        attributes.add(new UsedMemory());
        String gcBeanName = "java.lang:type=GarbageCollector,name=PS MarkSweep";
        attributes.add(new JmxAttribute(gcBeanName, "CollectionCount", "GCCollectionCount"));
        attributes.add(new JmxAttribute(gcBeanName, "CollectionTime", "GCCollectionTime"));
      } catch (MalformedObjectNameException e) {
        e.printStackTrace();
      }
    }

    if (params.hasOption("bean") && params.hasOption("attrs")) {
      attributes.addAll(JmxAttribute.getAttributesForBean(params.getOptionValue("bean"),
          Arrays.asList(params.getOptionValues("attrs"))));
    }
    if (params.hasOption("config")) {
      attributes.addAll(JmxAttribute.getAttributesFromConfig(params.getOptionValue("config")));
    }
    if (params.hasOption("interval")) {
      interval = Long.parseLong(params.getOptionValue("interval"));
    } else {
      interval = 1_000L;
    }
    if (params.hasOption("output")) {
      writers.add(new CsvWriter(params.getOptionValue("output")));
    }

  }

  private CommandLine getCliOptions(String[] args) {
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

    Option pidOption = Option.builder("p").longOpt("pid").argName("pid").type(Integer.class)
        .hasArg().desc("Java process id to monitor").build();
    Option beanOption = Option.builder("b").longOpt("bean").argName("bean").hasArg()
        .desc("Name of the bean to monitor").build();
    Option attributesOption = Option.builder("a").longOpt("attrs").argName("attributes").hasArgs()
        .desc("Names of attributes in bean to monitor").build();
    Option intervalOption = Option.builder("i").longOpt("interval").argName("interval").hasArg()
        .desc("Interval in ms to get next set of values").build();
    Option outputOption = Option.builder("o").longOpt("output").argName("output").hasArg()
        .desc("output file").build();
    Option memOption = Option.builder("m").longOpt("memory").argName("memory")
        .desc("logs used heap").build();
    Option configOption = Option.builder("c").longOpt("config").argName("config").hasArg()
        .desc("config file including an attribute per line").build();

    ops.addOption(pidOption);
    ops.addOption(beanOption);
    ops.addOption(attributesOption);
    ops.addOption(intervalOption);
    ops.addOption(outputOption);
    ops.addOption(memOption);
    ops.addOption(configOption);

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

  private void execute() {
    MBeanServerConnection connection = getBeanServerConnection();

    String header = "timestamp\t";

    for (JmxAttribute att : attributes) {
      header += att.getHeader() + "\t";
    }

    writeLine(header);
    System.out.print("\n");

    while (true) {
      String line = new Date().getTime() + "\t";
      for (JmxAttribute att : attributes) {
        line += att.getValue(connection) + "\t";
      }

      writeLine(line);
      try {
        Thread.sleep(interval);
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
    }
  }

  private void writeLine(String line) {
    for (OutputWriter writer : writers) {
      writer.writeLine(line);
    }
  }

  private MBeanServerConnection getBeanServerConnection() {
    Map<Integer, LocalVirtualMachine> allJvms = LocalVirtualMachine.getAllVirtualMachines();
    MBeanServerConnection connection = null;
    try {
      if (pid == -1) {
        System.out.println("Looking for Tomcat to connect...");
        List<Entry<Integer, LocalVirtualMachine>> candidates = new ArrayList<>();
        for (Entry<Integer, LocalVirtualMachine> jvm : allJvms.entrySet()) {
          if (jvm.getValue().displayName().contains("org.apache.catalina.startup.Bootstrap")) {
            candidates.add(jvm);
          }
        }
        switch (candidates.size()) {
        case 0:
          System.out.println("Didn't find any Tomcat instance");
        case 1:
          pid = candidates.get(0).getKey();
          System.out.println("Found Tomcat instance with pid " + pid);
          break;
        default:
          System.out.println("Found more than one Tomcat:");
          break;
        }
      }
      if (!allJvms.containsKey(pid)) {
        if (pid != -1) {
          System.err.println("pid " + pid + " not found");
          System.err.println("  detected jvms are ");
        }
        for (Entry<Integer, LocalVirtualMachine> jvm : allJvms.entrySet()) {
          System.err.println("    * " + jvm.getKey() + "\t- " + jvm.getValue().displayName());
        }
        System.exit(1);
      }

      LocalVirtualMachine jvm = LocalVirtualMachine.getLocalVirtualMachine(pid);
      System.out.println("Connected to " + pid + ": " + jvm.displayName());
      jvm.startManagementAgent();
      if (!jvm.isManageable()) {
        System.err.println("Cannot manage jvm");
        System.exit(1);
      }

      JMXServiceURL jmxService = new JMXServiceURL(jvm.connectorAddress());
      connection = JMXConnectorFactory.connect(jmxService, null).getMBeanServerConnection();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    return connection;
  }
}
