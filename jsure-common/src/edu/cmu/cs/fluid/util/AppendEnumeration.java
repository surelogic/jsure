/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/AppendEnumeration.java,v 1.7 2005/05/25 18:03:35 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

@Deprecated
@SuppressWarnings("all")
public class AppendEnumeration implements Enumeration {
  private Enumeration enm;
  private final Enumeration enm2;
  
  public AppendEnumeration(Enumeration e1, Enumeration e2) {
    enm  = e1;
    enm2 = e2;
  }
  
  public static Enumeration append(Enumeration e1, Enumeration e2) {
    if (e1 == null || !e1.hasMoreElements()) return e2;
    if (e2 == null || !e2.hasMoreElements()) return e1;
    return new AppendEnumeration(e1,e2);
  }
  
  public boolean hasMoreElements() {
    if (enm.hasMoreElements()) {
      return true;
    }
    enm = enm2;
    return enm.hasMoreElements();
  }

  public Object nextElement() {
    if (hasMoreElements()) {
      return enm.nextElement();
    }
    throw new NoSuchElementException();
  }

  Enumeration simplify() {
    if (enm == enm2) {
      if (enm2 instanceof AppendEnumeration)
	return ((AppendEnumeration)enm2).simplify();
      else
	return enm2;
    } else {
      return this;
    }
  }
}
