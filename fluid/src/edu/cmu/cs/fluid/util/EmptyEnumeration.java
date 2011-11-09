/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/EmptyEnumeration.java,v 1.4 2005/06/13 19:04:11 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

public class EmptyEnumeration implements Enumeration {
  public static final EmptyEnumeration prototype = new EmptyEnumeration();

  public EmptyEnumeration() { }

  public boolean hasMoreElements() {
    return false;
  }

  public Object nextElement() {
    throw new NoSuchElementException("enumeration complete");
  }
}
