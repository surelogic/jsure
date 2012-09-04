/*
 * Created on Feb 17, 2005
 *
 */
package edu.cmu.cs.fluid.util;

import java.util.*;

import edu.cmu.cs.fluid.util.IMultiMap.Entry;

import junit.framework.TestCase;


/**
 * @author Edwin
 *
 */
public class TestMultiMap extends TestCase {
  public void testMap() {
    IMultiMap<String,String> m = new SetMultiMap<String,String>();
    m.map("hi", "goodbye");
    m.map("hi", "hi");
    m.map("hi", "goodbye");
    m.map("bye", "see you");
    m.map("bye", "ttyl");
    for(Entry<String,String> e : m.entrySet()) {
      System.out.println("For "+e.getKey());
      Iterator<String> i = e.getValues().iterator();
      while (i.hasNext()) {
        System.out.println("\t"+i.next());
      }
    }
    
    Collection<String> s = m.find("hi");
    assertEquals(2, s.size());
    m.remove("hi", "huh?");
    assertEquals(2, s.size());
    s = m.find("hi");
    assertEquals(2, s.size());
    m.removeAll("hi");
    s = m.find("hi");
    assertEquals(null, s);
  }

  public void testFind() {
    IMultiMap<String,String> m = new SetMultiMap<String,String>();
    assertEquals(null, m.find("hi"));
  }
}
