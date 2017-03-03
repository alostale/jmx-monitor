package org.alostale.jmxmonitor;

import java.io.IOException;
import java.util.Arrays;
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
  private String bean;
  private List<String> attributes;

  public static void main(String[] args) throws ParseException {
    Monitor monitor = new Monitor(args);
    monitor.execute();
  }

  public Monitor(String[] args) {
    CommandLine params = getCliOptions(args);
    pid = Integer.parseInt(params.getOptionValue("pid"));
    bean = params.getOptionValue("bean");
    attributes = Arrays.asList(params.getOptionValues("attrs"));
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
    Option beanOption = Option.builder("b").longOpt("bean").argName("bean").hasArg().required()
        .desc("Name of the bean to monitor").build();
    Option attributesOption = Option.builder("a").longOpt("attrs").argName("attributes").hasArgs()
        .required().desc("Names of attributes in bean to monitor").build();

    ops.addOption(pidOption);
    ops.addOption(beanOption);
    ops.addOption(attributesOption);

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
    try {
      ObjectName beanName = new ObjectName(bean);

      for (String att : attributes) {
        try {
          System.out.println(bean + " " + att + ": " + connection.getAttribute(beanName, att));
        } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException
            | ReflectionException | IOException e) {
          e.printStackTrace();
        }
      }
    } catch (MalformedObjectNameException e1) {
      e1.printStackTrace();
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
