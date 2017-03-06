package org.alostale.jmxmonitor;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import sun.tools.jconsole.LocalVirtualMachine;

public class Monitor {
  private int pid;
  private String bean = "";
  private List<String> attributes = new ArrayList<>();
  private long interval;
  private BufferedOutputStream fileStream;
  private boolean logMemory = false;

  public static void main(String[] args) throws ParseException {
    Monitor monitor = new Monitor(args);
    monitor.execute();
  }

  public Monitor(String[] args) {
    CommandLine params = getCliOptions(args);
    pid = Integer.parseInt(params.getOptionValue("pid"));
    if (params.hasOption("bean") && params.hasOption("attrs")) {
      bean = params.getOptionValue("bean");
      attributes = Arrays.asList(params.getOptionValues("attrs"));
    }
    if (params.hasOption("interval")) {
      interval = Long.parseLong(params.getOptionValue("interval"));
    } else {
      interval = 1_000L;
    }
    if (params.hasOption("output")) {
      Path p = Paths.get(params.getOptionValue("output"));
      try {
        fileStream = new BufferedOutputStream(Files.newOutputStream(p, CREATE, APPEND));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if (params.hasOption("memory")) {
      logMemory = true;
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
        .hasArg().required().desc("Java process id to monitor").build();
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

    ops.addOption(pidOption);
    ops.addOption(beanOption);
    ops.addOption(attributesOption);
    ops.addOption(intervalOption);
    ops.addOption(outputOption);
    ops.addOption(memOption);

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
    if (logMemory) {
      header += "HeapMemoryUsage\t";
      header += "GCCollectionCount\t";
      header += "GCCollectionTime\t";
    }
    for (String att : attributes) {
      header += att + "\t";
    }

    writeLine(header);
    System.out.print("\n");

    try {
      ObjectName beanName = new ObjectName(bean);
      ObjectName memoryBeanName = new ObjectName("java.lang:type=Memory");
      ObjectName gcBeanName = new ObjectName("java.lang:type=GarbageCollector,name=PS MarkSweep");
      while (true) {
        String line = new Date().getTime() + "\t";
        try {
          if (logMemory) {
            line += ((CompositeDataSupport) connection.getAttribute(memoryBeanName,
                "HeapMemoryUsage")).get("used") + "\t";
            line += connection.getAttribute(gcBeanName, "CollectionCount") + "\t";
            line += connection.getAttribute(gcBeanName, "CollectionTime") + "\t";
          }
          for (String att : attributes) {
            line += connection.getAttribute(beanName, att) + "\t";
          }
        } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException
            | ReflectionException | IOException e) {
          e.printStackTrace();
        }
        writeLine(line);
        System.out.print("\r");
        Thread.sleep(interval);
      }
    } catch (MalformedObjectNameException | InterruptedException e1) {
      e1.printStackTrace();
    }
  }

  private void writeLine(String line) {
    System.out.print(line);

    String csvLine = line.replace("\t", ",");
    if (csvLine.endsWith(",")) {
      csvLine = csvLine.substring(0, csvLine.length() - 1);
    }
    csvLine += "\n";
    if (fileStream != null) {
      try {
        fileStream.write(csvLine.getBytes(), 0, csvLine.length());
        fileStream.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

  private MBeanServerConnection getBeanServerConnection() {
    Map<Integer, LocalVirtualMachine> allJvms = LocalVirtualMachine.getAllVirtualMachines();
    MBeanServerConnection connection = null;
    try {
      if (!allJvms.containsKey(pid)) {
        System.err.println("pid " + pid + " not found");
        System.err.println("  detected jvms are ");
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
