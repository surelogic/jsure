/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/PairEnumeration.java,v 1.3 2005/05/25 18:03:35 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;
@Deprecated
@SuppressWarnings("all")
public class PairEnumeration implements Enumeration {
  private int done = 0;
  private final Object value1, value2;

  public PairEnumeration(Object v1, Object v2) {
    value1 = v1;
    value2 = v2;
  }

  public boolean hasMoreElements() {
    return done < 2;
  }

  public Object nextElement() {
    switch (++done) {
    case 1: return value1;
    case 2: return value2;
    }
    throw new NoSuchElementException("enumeration complete");
  }
}

