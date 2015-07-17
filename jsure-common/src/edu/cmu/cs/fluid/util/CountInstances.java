/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/CountInstances.java,v 1.7 2007/07/05 18:15:13 aarong Exp $
 */
package edu.cmu.cs.fluid.util;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import com.surelogic.*;

/**
 * Keeping track of the number of things created. Inherit from this class, or
 * else add code to call "add"
 * 
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
  private static Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();

  @Unique("return")
  public CountInstances() {
    add(this);
  }

  public static void add(@Borrowed Object x) {
    String n = x.getClass().getName();
    AtomicInteger c = counts.get(n);
    if (c == null) {
      c = new AtomicInteger();
      counts.put(n, c);
    }
    c.incrementAndGet();
  }

  public static void reset() {
    for (Map.Entry<String, AtomicInteger> e : counts.entrySet()) {
      AtomicInteger c = e.getValue();
      c.set(0);
    }
  }

  public static void report() {
    for (Map.Entry<String, AtomicInteger> e : counts.entrySet()) {
      System.out.println(e.getKey() + ": " + e.getValue().intValue());
    }
  }
}
