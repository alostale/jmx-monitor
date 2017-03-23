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
    beanName = new ObjectName(bean);
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
        configLine = configLine.trim();
        if (configLine.startsWith("#")) {
          continue;
        }
        String[] config = configLine.split(" ");
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
