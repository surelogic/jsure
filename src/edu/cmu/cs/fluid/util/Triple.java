/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/Triple.java,v 1.4 2005/06/30 21:45:40 chance Exp $ */
package edu.cmu.cs.fluid.util;

/**
 * A simple triple, defined so we can define equality and hashCode. Changed to
 * also cache the hashCode (assuming that it's immutable)
 */
public class Triple<T1, T2, T3> {
  private final T1 elem1;

  private final T2 elem2;

  private final T3 elem3;

  private final int hash;

  public Triple(T1 o1, T2 o2, T3 o3) {
    elem1 = o1;
    elem2 = o2;
    elem3 = o3;
    hash = computeHash();
  }

  public final T1 first() {
    return elem1;
  }

  public final T2 second() {
    return elem2;
  }

  public final T3 third() {
    return elem3;
  }

  @Override
  public final boolean equals(Object other) {
    if (other instanceof Triple) {
      @SuppressWarnings("unchecked")
      final Triple otherT = (Triple) other;
      return elem1.equals(otherT.elem1) && elem2.equals(otherT.elem2)
          && elem3.equals(otherT.elem3);
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return hash;
  }

  private int computeHash() {
    int hash = 0;
    if (elem1 != null) {
      hash += elem1.hashCode();
    }
    if (elem2 != null) {
      hash += elem2.hashCode();
    }
    if (elem3 != null) {
      hash += elem3.hashCode();
    }
    return hash;
  }
}
