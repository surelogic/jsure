/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/MutablePair.java,v 1.1 2008/08/19 15:31:59 chance Exp $ */
package edu.cmu.cs.fluid.util;

public class MutablePair<T1,T2> {
  private T1 elem1;
  private T2 elem2;
  public MutablePair(T1 o1, T2 o2) {
    elem1 = o1;
    elem2 = o2;
  }
  public T1 first() { return elem1; }
  public T2 second() { return elem2; }
  
  public void setFirst(T1 val) {
	  elem1 = val;
  }
  
  public void setSecond(T2 val) {
	  elem2 = val;
  }
}
