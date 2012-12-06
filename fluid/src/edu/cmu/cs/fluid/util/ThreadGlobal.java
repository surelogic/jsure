/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/ThreadGlobal.java,v 1.8
 * 2003/07/02 20:19:04 thallora Exp $
 */
package edu.cmu.cs.fluid.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.*;
import com.surelogic.common.logging.SLLogger;

/**
 * A global variable whose value may differ from thread to thread. We cache the
 * latest value to speed access. The value "null" is a legal default and a
 * lgeal value to a variable to.
 * 
 * "Private" inheritance of ThreadLocal Invariant of having a fixed stack
 * "head" (if created), to avoid get/set() Abstraction of having an infinite
 * stack, filled w/ default value
 */
@ThreadSafe
@Region("private ReclaimState")
@RegionLock("ReclaimLock is this protects ReclaimState")
public class ThreadGlobal<T> extends ThreadLocal<ThreadGlobal.Element<T>> {
  /**
	 * Logger for this class
	 */
  private static final Logger LOG = SLLogger.getLogger("FLUID.util");

  private final T defaultValue;

  static class Element<V> {
	@Unique("return")
	Element() {
		// Nothing to do
	}
	  
    V val;
    Element<V> next;
  }
  @InRegion("ReclaimState")
  private Element<T> reclaimed;

  /**
	 * Create a new thread global variable.
	 * 
	 * @param def
	 *          default value in case not set for thread.
	 */
  @Unique("return")
  public ThreadGlobal(T def) {
    defaultValue = def;
  }

  /**
	 * Get the current value of this global for the current thread. If not set
	 * for this thread, use the default value.
	 * 
	 * No need to synchronize since we're only operating on the stack for this
	 * thread
	 * 
	 * @see #setValue
	 * @see #popValue
	 */
  public T getValue() {
    Element<T> elt = get();
    return (elt == null) ? defaultValue : elt.val;
  }

  /**
	 * Set the current value of this global for the current thread.
	 * 
	 * @return the old value (the default, if not set yet).
	 */
  public Object setValue(T newValue) {
    Element<T> elt = get();
    if (elt == null) {
      // initialize the stack
      elt = newElement(null);
      set(elt);
      elt.val = newValue;
      return defaultValue;
    }
    Object o = elt.val;
    elt.val = newValue;
    return o;
  }

  private synchronized Element<T> newElement(Element<T> next) {
    Element<T> elt;
    if (reclaimed == null) {
      elt = new Element<T>();
    } else {
      elt = reclaimed;
      reclaimed = elt.next;
    }
    elt.next = next;
    return elt;
  }

  /**
	 * @param elt
	 */
  private synchronized void reclaimElement(Element<T> elt) {
    elt.next = reclaimed;
    reclaimed = elt;
  }

  /**
	 * Set the current value while remembering the current value. Must be matched
	 * by a popValue in the same thread.
	 * 
	 * @see #popValue()
	 */
  public void pushValue(T newValue) {
    /*
		 * Element elt = newElement(get()); elt.val = newValue; set(elt); // TODO
		 * fix get/set -> get()
		 */
    Element<T> head = get();
    if (head == null) {
      // just like the original code
      Element<T> elt = newElement(null);
      elt.val = newValue;
      set(elt);
    } else {
      // copy head to a new element after itself
      Element<T> next = newElement(head.next);
      next.val = head.val;

      // set head to its new values
      // avoiding set()
      head.val = newValue;
      head.next = next;
    }
  }

  /**
	 * Restore the remembered value of the last pushValue. Must have been matched
	 * by a pushValue in the same thread. We do <em>not</em> require proper
	 * nesting between threads.
	 * 
	 * @return value before pop.
	 */
  public Object popValue() {
    Element<T> elt = get();
    if (elt == null) {
      LOG.log(Level.SEVERE, "Extra pop()", new Throwable("For stack trace"));
      return defaultValue;
    }
    // elt is non-null
    final Object o = elt.val;
    /*
		 * set(elt.next); reclaimElement(elt);
		 */
    final Element<T> next = elt.next;
    if (next == null) {
      elt.val = defaultValue;
    } else {
      // next is non-null
      elt.val = next.val;
      elt.next = next.next;
      reclaimElement(next);
    }
    return o;
  }

  @Override
  public String toString() {
    Element<T> head = get();
    StringBuilder sb = new StringBuilder();
    sb.append(Thread.currentThread() + " => {");
    for (Element<T> elt = head; elt != null; elt = elt.next) {
      if (elt != head)
        sb.append(",");
      sb.append(elt.val);
    }
    sb.append("}");
    return sb.toString();
  }

  public static void main(String[] args) {
    ThreadGlobal<String> local = new ThreadGlobal<String>("a");
    OldThreadGlobal global = new OldThreadGlobal("a");

    if (!global.getValue().equals(local.getValue())) {
      System.out.println("Failed to get default value");
    }
    global.setValue("b");
    local.setValue("b");
    if (!global.getValue().equals(local.getValue())) {
      System.out.println("Failed to set value");
    }
    global.pushValue("c");
    local.pushValue("c");
    if (!global.getValue().equals(local.getValue())) {
      System.out.println("Failed to push value");
    }
    Object g = global.popValue();
    Object l = local.popValue();
    if (!g.equals(l)) {
      System.out.println("Failed to pop value");
    }
    if (!global.getValue().equals(local.getValue())) {
      System.out.println("Failed to get value");
      System.out.println(global.getValue());
      System.out.println(local.getValue());
    }
    global.pushValue("d");
    local.pushValue("d");
    if (!global.getValue().equals(local.getValue())) {
      System.out.println("Failed to push value 2");
    }
    global.pushValue("e");
    local.pushValue("e");
    if (!global.getValue().equals(local.getValue())) {
      System.out.println("Failed to push value 3");
    }
    global.setValue("f");
    local.setValue("f");
    if (!global.getValue().equals(local.getValue())) {
      System.out.println("Failed to set value 2");
    }
    g = global.popValue();
    l = local.popValue();
    if (!g.equals(l)) {
      System.out.println("Failed to pop value 2");
    }
    if (!global.getValue().equals(local.getValue())) {
      System.out.println("Failed to get value 2");
      System.out.println(global.getValue());
      System.out.println(local.getValue());
    }

    System.out.println("Test completed.");
  }
}

/**
 * A global variable whose value may differ from thread to thread. We cache the
 * latest value to speed access. The value "null" is a legal default and a
 * lgeal value to a variable to.
 * 
 * @lock L is this protects Instance
 */
class OldThreadGlobal {
  private Object defaultValue;
  private ThreadBinding bindings;

  /**
   * Create a new thread global variable.
   * 
   * @param def
   *          default value in case not set for thread.
   * @singleThreaded
   * @borrowed this
   */
  public OldThreadGlobal(Object def) {
    defaultValue = def;
    bindings = new ThreadBinding(Thread.currentThread(), def, null);
  }

  /**
	 * Get the current value of this global for the current thread. If not set
	 * for this thread, use the default value. We also move the value up to the
	 * front for faster access next time. This useful feature is required to make
	 * setValue and popValue work.
	 * 
	 * @see #setValue
	 * @see #popValue
	 */
  public synchronized Object getValue() {
    Thread t = Thread.currentThread();
    ThreadBinding last = null;
    for (ThreadBinding b = bindings; b != null; b = b.next) {
      if (b.thread == t) {
        if (last != null) { // reorder
          last.next = b.next;
          b.next = bindings;
          bindings = b;
        }
        return b.value;
      }
      last = b;
    }
    bindings = new ThreadBinding(t, defaultValue, bindings);
    return defaultValue;
  }

  /**
	 * Set the current value of this global for the current thread.
	 * 
	 * @return the old value (the default, if not set yet).
	 */
  public synchronized Object setValue(Object newValue) {
    Object oldValue = getValue();
    bindings.value = newValue;
    return oldValue;
  }

  /**
	 * Set the current value while remembering the current value. Must be matched
	 * by a popValue in the same thread.
	 * 
	 * @see #popValue()
	 */
  public synchronized void pushValue(Object newValue) {
    Thread t = Thread.currentThread();
    // bindings = new ThreadBinding(t,newValue,bindings);
    bindings = ThreadBinding.newBinding(t, newValue, bindings);
  }

  /**
	 * Restore the remembered value of the last pushValue. Must have been matched
	 * by a pushValue in the same thread. We do <em>not</em> require proper
	 * nesting between threads.
	 * 
	 * @return value before pop.
	 */
  public synchronized Object popValue() {
    Object oldValue = getValue();
    ThreadBinding tb = bindings;
    bindings = bindings.next;
    ThreadBinding.reclaimBinding(tb);
    return oldValue;
  }

  @Override
  public synchronized String toString() {
    getValue(); // force current thread to front
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    for (ThreadBinding b = bindings; b != null; b = b.next) {
      if (b != bindings)
        sb.append(",");
      sb.append(b.thread);
      sb.append("=>");
      sb.append(b.value);
    }
    sb.append("}");
    return sb.toString();
  }
}

class ThreadBinding {
  Thread thread;
  Object value;
  ThreadBinding next = null;

  ThreadBinding(Thread t, Object v, ThreadBinding b) {
    thread = t;
    value = v;
    next = b;
  }

  // reusing objects
  static ThreadBinding prealloc = null;

  static ThreadBinding newBinding(Thread t, Object v, ThreadBinding b) {
    if (prealloc == null) {
      return new ThreadBinding(t, v, b);
    }
    // take off stack
    ThreadBinding tb = prealloc;
    prealloc = tb.next;

    // init object
    tb.thread = t;
    tb.value = v;
    tb.next = b;
    return tb;
  }

  static void reclaimBinding(ThreadBinding tb) {
    tb.next = prealloc;
    prealloc = tb;
  }
}
