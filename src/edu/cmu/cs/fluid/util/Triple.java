/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/Triple.java,v 1.4 2005/06/30 21:45:40 chance Exp $ */
package edu.cmu.cs.fluid.util;

/** A simple triple, defined so we can define equality and hashCode. */
public class Triple<T1,T2,T3> {
  private final T1 elem1;
  private final T2 elem2;
  private final T3 elem3;
  public Triple(T1 o1, T2 o2, T3 o3) {
    elem1 = o1;
    elem2 = o2;
    elem3 = o3;
  }
  public T1 first() { return elem1; }
  public T2 second() { return elem2; }
  public T3 third() { return elem3; }
  @Override
  public boolean equals(Object other) {
    return other instanceof Triple &&
      elem1.equals(((Triple)other).elem1) &&
      elem2.equals(((Triple)other).elem2) &&
      elem3.equals(((Triple)other).elem3);
  }
  @Override
  public int hashCode() {
    return elem1.hashCode() + elem2.hashCode() + elem3.hashCode();
  }
}
