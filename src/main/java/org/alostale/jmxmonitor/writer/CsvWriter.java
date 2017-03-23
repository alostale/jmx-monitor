package org.alostale.jmxmonitor.writer;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CsvWriter implements OutputWriter {
  private BufferedOutputStream fileStream;

  public CsvWriter(String filePath) {
    Path p = Paths.get(filePath);
    try {
      fileStream = new BufferedOutputStream(Files.newOutputStream(p, CREATE, APPEND));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void writeLine(String line) {
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

}
