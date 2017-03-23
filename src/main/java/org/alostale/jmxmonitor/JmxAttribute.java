package org.alostale.jmxmonitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class JmxAttribute {
  private ObjectName beanName;
  private String attributeName;
  private String alias;

  public JmxAttribute(String bean, String attribute) throws MalformedObjectNameException {
    this(bean, attribute, null);
  }

  public JmxAttribute(String bean, String attribute, String alias)
      throws MalformedObjectNameException {
    System.out.println("bean: " + bean + " - attribute: " + attribute
        + ((alias != null && !alias.isEmpty()) ? (" - alias: " + alias) : ""));
    try {
      beanName = new ObjectName(bean);
    } catch (MalformedObjectNameException e) {
      System.err.println("Malformed bean name: " + bean);
      throw (e);
    }
    attributeName = attribute;
    this.alias = (alias == null || alias.isEmpty()) ? attributeName : alias;
  }

  public ObjectName getBeanName() {
    return beanName;
  }

  public String getHeader() {
    return alias;
  }

  public Object getValue(MBeanServerConnection connection) {
    try {
      return connection.getAttribute(beanName, attributeName);
    } catch (AttributeNotFoundException | InstanceNotFoundException | MBeanException
        | ReflectionException | IOException e) {
      e.printStackTrace();
      return "";
    }
  }

  public static List<JmxAttribute> getAttributesForBean(String beanName,
      List<String> attributeNames) {
    List<JmxAttribute> attributes = new ArrayList<>();

    for (String attName : attributeNames) {
      try {
        attributes.add(new JmxAttribute(beanName, attName));
      } catch (MalformedObjectNameException e) {
        e.printStackTrace();
      }
    }
    return attributes;
  }

  public static List<JmxAttribute> getAttributesFromConfig(String configFilePath) {
    List<JmxAttribute> attributes = new ArrayList<>();
    try {
      for (String configLine : Files.readAllLines(Paths.get(configFilePath))) {
        configLine = configLine.trim().replaceAll("\\s\\s+", " ");
        String[] config = configLine.split(" ");
        if (configLine.startsWith("#") || config.length < 2) {
          System.out.println(configLine);
          continue;
        }

        String beanName = config[0].trim();
        String attributeName = config[1].trim();
        String alias = config.length > 2 ? config[2] : null;
        attributes.add(new JmxAttribute(beanName, attributeName, alias));
      }
    } catch (IOException | MalformedObjectNameException e) {
      e.printStackTrace();
    }

    return attributes;
  }
}
