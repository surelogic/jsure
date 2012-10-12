/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/logging/XMLLogDiff.java,v 1.9 2007/08/30 18:41:45 chance Exp $*/
package edu.cmu.cs.fluid.logging;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.xml.XMLLayout;

import com.surelogic.common.Pair;
import com.surelogic.test.ITest;
import com.surelogic.test.ITestOutput;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.util.*;

public class XMLLogDiff {
  public static final String FLUID_COUNT = "fluid.count";

  static class EventState {
    final LoggingEvent event;
    private int count = 0;
    
    EventState(LoggingEvent e) {
      event = e;
    }
    
    int count() { return count; }
    
    int incr() {
      if (count < 0) {
        new Throwable("Incrementing a negative count:").printStackTrace();        
      }
      count++;
      return count;
    }
    
    int decr() {
      count--;
      return count;
    }
  }
  
  private static List<LoggingEvent> getEventsFromLog(String name) 
  throws MalformedURLException {
    List<LoggingEvent> events = Collections.emptyList();
    File f                    = new File(name);
    if (f.exists()) {
      String file = "file:///"+name.replace(":", "|");
      System.out.println("Getting events from "+file);

      URL url             = new URL(file);
      XMLLogReader reader = new XMLLogReader();    
      try {
        events = reader.readURL(url);
      } 
      catch (NullPointerException e) {
        // FIX workaround for problem with Chainsaw
      }    
    }
    System.out.println("Read "+events.size()+" events for "+name);
    return events;
  }
  
  private static EventState makeStateInTable(Map<Pair<String,String>,EventState> table, LoggingEvent e) {
    final String info = e.getLocationInformation().getFullInfo();
    final String msg  = e.getRenderedMessage();
    final Pair<String,String> key = Pair.getInstance(info, msg);
    EventState state  = table.get(key);
    if (state == null) {
      state = new EventState(e);
      if (table.put(key, state) != null) {
        // We shouldn't get this 
        System.err.println("Got duplicate log event: "+e.getRenderedMessage());
      }
    }
    return state;
  }
  
  /**
   * This is designed to deal with the fact that log events may not come in order
   * 
   * @return a table keyed on the location and message
   */
  private static HashMap<Pair<String,String>,EventState> convertToTable(List<LoggingEvent> log) {
    final HashMap<Pair<String,String>,EventState> table = new HashMap<Pair<String,String>, EventState>();
    for (LoggingEvent e : log) {
      final EventState state = makeStateInTable(table, e);
      state.incr();
    }
    return table;
  }
  
  private static HashMap<Pair<String,String>,EventState> getTableFromLog(String name) 
  throws MalformedURLException {  
    List<LoggingEvent> log = getEventsFromLog(name);
    return convertToTable(log);
  }

  private static boolean consideredSame(LoggingEvent e1, LoggingEvent e2) {
    if (!e1.getLocationInformation().getFullInfo().equals(e2.getLocationInformation().getFullInfo())) {
      return false;
    }
    if (!e1.getRenderedMessage().equals(e2.getRenderedMessage())) {
      return false;
    }
    return true;
  }
  
  /**
   * @return the number of different events
   * @throws IOException 
   */
  public static int diff(ITestOutput out, String oracleName, String logName, String logDiffsName) 
  throws IOException {
    Map<Pair<String,String>,EventState> log = getTableFromLog(logName);
    List<LoggingEvent> oracle               = getEventsFromLog(oracleName);
    
    System.out.println("Removing expected events");
    try {
      for (LoggingEvent e : oracle) {
        final EventState state = makeStateInTable(log, e);
        final String msg       = e.getRenderedMessage();
        final int count        = state.count;

        if (count == 0) {
          System.out.println("Log did not contain event: "+msg);
          state.decr();
        }
        else if (consideredSame(e, state.event)) {
          if (state.decr() < 0) {
            System.out.println("Log did not contain enough events of the form: "+msg);
          } else {
            System.out.println("Matched: "+msg);
          }
        }
        else {
          System.out.println("Not considered a match: "+msg);
          state.decr();
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
    // Anything left over in 'log' should be new ...
    for(final EventState e : log.values()) {
      ITest o = new ITest() {
        public String getClassName() {
          return e.event.getLocationInformation().className;
        }
        public IRNode getNode() {
          return null;
        }
		public String identity() {
			return toString();
		} 
      };
      out.reportStart(o);
      if (e.event.getThrowableInformation() != null) { 
        @SuppressWarnings("deprecation")
        Throwable t = e.event.getThrowableInformation().getThrowable();
        if (t != null) {
          out.reportError(o, t);
        } else {
          String[] msgs    = e.event.getThrowableInformation().getThrowableStrRep();
          StringBuilder sb = new StringBuilder();
          sb.append(e.event.getRenderedMessage());
          for(String msg : msgs) {
            sb.append(msg);
          }
          out.reportFailure(o, sb.toString());
        }
      } else {
        out.reportFailure(o, e.event.getRenderedMessage());
      }
    }
    return createXMLLog(logDiffsName, log.values());
  }

  private static int createXMLLog(String name, Iterable<EventState> log) throws FileNotFoundException {
    FileOutputStream f = new FileOutputStream(name);
    PrintWriter pw     = new PrintWriter(f);
    XMLLayout layout   = new XMLLayout();
    
    int diffs = 0;
    for (EventState s : log) {
      final int count = s.count();
      if (count != 0) {
        s.event.setProperty(FLUID_COUNT, Integer.toString(count));
        
        String event = layout.format(s.event);    
        pw.println(event); 
        diffs += count;
        System.out.println("Diff: "+count+" "+s.event.getRenderedMessage());
      }
    }
    pw.close();
    return diffs;
  }


}
