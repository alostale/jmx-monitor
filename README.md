# jmx-monitor
Simple CLI jmx monitor. Monitors some attributes for a given JMX bean, writing contents in a csv file.

Example of usage.
```
java -cp $JAVA_HOME/lib/tools.jar:$JAVA_HOME/lib/jconsole.jar:jmxmonitor-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.alostale.jmxmonitor.Monitor -p 16808 -b Openbravo:name=Pool-DEFAULT,type=openbravo --attrs Active Idle Size WaitCount -o /tmp/m.log
```
