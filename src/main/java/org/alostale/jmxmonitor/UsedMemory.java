package org.alostale.jmxmonitor;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.openmbean.CompositeDataSupport;

public class UsedMemory extends JmxAttribute {

  private UsedMemory(String bean, String attribute) throws MalformedObjectNameException {
    super(bean, attribute);
  }

  public UsedMemory() throws MalformedObjectNameException {
    super("java.lang:type=Memory", "HeapMemoryUsage", "UsedHeap");
  }

  @Override
  public Object getValue(MBeanServerConnection connection) {
    return ((CompositeDataSupport) super.getValue(connection)).get("used");
  }

}
