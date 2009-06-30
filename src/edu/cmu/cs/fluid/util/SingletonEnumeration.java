/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/SingletonEnumeration.java,v 1.4 2005/05/25 18:03:35 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

@Deprecated
@SuppressWarnings("all")
public class SingletonEnumeration implements Enumeration {
  private boolean done = false;
  private final Object value;

  public SingletonEnumeration(Object v) {
    value = v;
  }

  public boolean hasMoreElements() {
    return !done;
  }

  public Object nextElement() {
    if (done) throw new NoSuchElementException("enumeration complete");
    done = true;
    return value;
  }
}

