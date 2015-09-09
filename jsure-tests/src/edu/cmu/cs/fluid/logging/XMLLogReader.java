/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/logging/XMLLogReader.java,v 1.3 2007/11/15 14:51:42 chance Exp $*/
package edu.cmu.cs.fluid.logging;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.xml.UtilLoggingXMLDecoder;

public class XMLLogReader {
  private final UtilLoggingXMLDecoder decoder = new UtilLoggingXMLDecoder();
  
  @SuppressWarnings("unchecked")
  public List<LoggingEvent> readURL(URL file) {
    try {
      Vector v = decoder.decode(file);
      return v;
    } catch (NullPointerException e) {
      //e.printStackTrace();
      System.out.println("NPE: probably due to no events in log");
      return Collections.emptyList();
    } catch (IOException e) {
      e.printStackTrace();
      return Collections.emptyList();
    }
  }
}
