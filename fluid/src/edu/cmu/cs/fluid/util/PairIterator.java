/* $Header$ */
package edu.cmu.cs.fluid.util;

import java.util.NoSuchElementException;

public class PairIterator<T> extends AbstractRemovelessIterator<T> {
  private int done = 0;
  private final T value1, value2;

  public PairIterator(T v1, T v2) {
    value1 = v1;
    value2 = v2;
  }

  @Override
  public boolean hasNext() {
    return done < 2;
  }

  @Override
  public T next() {
    switch (++done) {
    case 1: return value1;
    case 2: return value2;
    }
    throw new NoSuchElementException("enumeration complete");
  }
}

