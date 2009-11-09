/*$Header: /cvs/fluid/fluid/src/com/surelogic/test/AbstractTestOutput.java,v 1.10 2008/07/07 20:49:14 chance Exp $*/
package com.surelogic.test;

import java.util.*;
import java.util.Map.Entry;

import edu.cmu.cs.fluid.util.*;

public abstract class AbstractTestOutput implements ITestOutput {
  private final Map<ITest,Object> tracker = new HashMap<ITest,Object>();
  private final String name;
  private boolean open = true;
  
  protected AbstractTestOutput(String n) {
    name = n;
  }
  
  private void checkIfOpen() {
    if (!open) {
      throw new Error("Already closed: "+name);
    }
  }
  
  public void reset() {
	  tracker.clear();
  }
  
  public ITest reportStart(ITest o) {
    if (tracker.containsKey(o)) {
      throw new IllegalArgumentException("Already started: "+o);
    }
    tracker.put(o, null);
    return o;
  }

  /**
   * @return true if reported successfully
   */
  protected boolean report(ITest o, Object ex) {
    checkIfOpen();
    if (!tracker.containsKey(o)) {
      throw new IllegalArgumentException("Reported on non-existent "+o);
    }
    /*
    if ("@Unique  a, b, c matched UNPARSEABLE".equals(ex)) {
      System.err.println("Matched: "+o.hashCode());
    }
    */
    Object last = tracker.put(o, ex);
    if (last != null) {
      if (last.equals(ex)) {
        //System.err.println("WARNING: got message twice: "+last);
        return false;
      } else {
        throw new IllegalArgumentException("Already reported on "+o.hashCode());
      }
    }
    return true;
  }

  public Iterable<Object> getUnreported() {
    // copied to avoid ConcurrentModEx
    final List<Entry<ITest,Object>> list = new ArrayList<Entry<ITest,Object>>(tracker.entrySet());
    final Iterator<Entry<ITest,Object>> entries = list.iterator();
    return new SimpleRemovelessIterator<Object>() {
      @Override
      protected Object computeNext() {
        while (entries.hasNext()) {
          Entry<ITest,Object> e = entries.next();
          if (e.getValue() == null) {
            return e.getKey();
          }
        }
        return IteratorUtil.noElement;
      }
    };
  }
  
  public void close() {
    for(ITest key : new ArrayList<ITest>(tracker.keySet())) {
      if (tracker.get(key) == null) {
        reportFailure(key, "No recorded result for "+key);
      }
    }
    tracker.clear();
//    System.out.println("Closing "+getClass().getName()+": "+name);
  }  
}
