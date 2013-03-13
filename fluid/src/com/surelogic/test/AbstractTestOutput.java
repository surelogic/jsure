/*$Header: /cvs/fluid/fluid/src/com/surelogic/test/AbstractTestOutput.java,v 1.10 2008/07/07 20:49:14 chance Exp $*/
package com.surelogic.test;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import com.surelogic.common.util.*;

public abstract class AbstractTestOutput implements ITestOutput {
  private static final Object noResult = new Object();
  private final ConcurrentMap<ITest,Object> tracker = new ConcurrentHashMap<ITest,Object>();
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
  
  @Override
  public void reset() {
	  tracker.clear();
  }
  
  @Override
  public ITest reportStart(ITest o) {
    Object prev = tracker.putIfAbsent(o, noResult);
    if (prev != null) {
    	throw new IllegalArgumentException("Already started: "+o);
    }
    //System.out.println(this+": started "+o.identity());
    return o;
  }

  /**
   * @return true if reported successfully
   */
  protected boolean report(ITest o, Object ex) {
    checkIfOpen();

    //System.out.println(this+": reporting "+o.identity());
    Object last = tracker.put(o, ex);
    if (last == null) {
    	throw new IllegalArgumentException("Reported on non-existent "+o);
    }
    if (last != noResult) {
      if (last.equals(ex)) {
        //System.err.println("WARNING: got message twice: "+last);
        return false;
      } else {
        throw new IllegalArgumentException("Already reported on "+o.hashCode());
      }
    }
    return true;
  }

  @Override
  public Iterable<Object> getUnreported() {
    // copied to avoid ConcurrentModEx
    final List<Entry<ITest,Object>> list = new ArrayList<Entry<ITest,Object>>(tracker.entrySet());
    final Iterator<Entry<ITest,Object>> entries = list.iterator();
    return new SimpleRemovelessIterator<Object>() {
      @Override
      protected Object computeNext() {
        while (entries.hasNext()) {
          Entry<ITest,Object> e = entries.next();
          if (e.getValue() == noResult) {
            return e.getKey();
          }
        }
        return IteratorUtil.noElement;
      }
    };
  }
  
  @Override
  public void close() {
	  final List<Entry<ITest,Object>> list = new ArrayList<Entry<ITest,Object>>(tracker.entrySet());
	  for(Entry<ITest,Object> e : list) {
		  final ITest key = e.getKey();
		  if (e.getValue() == noResult) {
			  reportFailure(key, "No recorded result for "+key);
		  }
	  }    
	  tracker.clear();
//    System.out.println("Closing "+getClass().getName()+": "+name);
  }  
}
