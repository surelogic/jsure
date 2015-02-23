/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/TripleIterator.java,v 1.1 2007/10/23 18:10:35 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.NoSuchElementException;

import com.surelogic.common.util.AbstractRemovelessIterator;

public class TripleIterator<T> extends AbstractRemovelessIterator<T> {
  private int done = 0;
  private final T value1, value2, value3;

  public TripleIterator(T v1, T v2, T v3) {
    value1 = v1;
    value2 = v2;
    value3 = v3;
  }

  public boolean hasNext() {
    return done < 3;
  }

  public T next() {
    switch (++done) {
    case 1: return value1;
    case 2: return value2;
    case 3: return value3;
    }
    throw new NoSuchElementException("enumeration complete");
  }
}

