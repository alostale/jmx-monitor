package org.alostale.jmxmonitor.writer;

import java.util.Arrays;

public class StdinWriter implements OutputWriter {

  @Override
  public void writeLine(String line) {
    resetLine();

    System.out.print(line);
    System.out.print("\r");
  }

  private void resetLine() {
    int numOfCols;
    try {
      numOfCols = Integer.parseInt(System.getenv("COLUMNS"));
    } catch (Exception e) {
      numOfCols = 80;
    }
    char[] charArray = new char[numOfCols];
    Arrays.fill(charArray, ' ');
    System.out.print(new String(charArray) + "\r");
  }
}
