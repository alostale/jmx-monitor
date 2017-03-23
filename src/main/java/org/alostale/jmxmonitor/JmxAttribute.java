package org.alostale.jmxmonitor;

import java.io.IOException;
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
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return attributes;
  }
}
