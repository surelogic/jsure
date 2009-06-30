/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/CountInstances.java,v 1.7 2007/07/05 18:15:13 aarong Exp $
 */
package edu.cmu.cs.fluid.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Keeping track of the number of things created.
 * Inherit from this class, or else add code to call "add"
 * @author boyland
 *
 * @region private static Counts
 * @lock CountLock is class protects Counts
 */
public class CountInstances {
  /**
   * @unique
   * @aggregate Instance into Counts  
   */
  private static Map<String,IntCell> counts = new HashMap<String,IntCell>();
  
  public CountInstances() {
    add(this);
  }
  
  public static synchronized void add(Object x) {
    String n = x.getClass().getName();
    IntCell c = counts.get(n);
    if (c == null) {
      c = new IntCell();
      counts.put(n,c);
    }
    ++c.value;
  }
  
  public static synchronized void reset() {
    for (Iterator it = counts.entrySet().iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry)it.next();
      IntCell c = (IntCell)e.getValue();
      c.value = 0;
    }
  }
  
  public static synchronized void report() {
    for (Iterator it = counts.entrySet().iterator(); it.hasNext();) {
      Map.Entry e = (Map.Entry)it.next();
      System.out.println(e.getKey() +": " + e.getValue());
    }
  }
  
  private static class IntCell {
    public int value;
    @Override
    public String toString() { return Integer.toString(value); }
  }
}
