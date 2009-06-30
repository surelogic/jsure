/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/FlatLattice.java,v 1.8 2008/12/12 19:01:02 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.Vector;

/** The trivial lattice with top and bottom around a flat domain.
 */
public class FlatLattice implements Lattice {
  public static final FlatLattice prototype =
      new FlatLattice(1,null);
  public static final FlatLattice topValue = prototype;
  public static final FlatLattice bottomValue =
      new FlatLattice(-1,null);

  private final int height;
  private final Object object;
  
  public FlatLattice(Object o) {
    // this(true,o);
    height = 0;
    object = o;
  }

  private FlatLattice(int h, Object o) {
    height = h;
    object = o;
  }

  /**
   * Test whether we have a "middle" element (neither top nor bottom).
   * @return true if neither top nor bottom
   */
  public boolean inDomain() { return height == 0; }
  public Object getValue() { return object; }

  @Override
  public boolean equals(Object other) {
    if (other instanceof FlatLattice) {
      FlatLattice fl = (FlatLattice)other;
      if (height != fl.height) return false;
      return height != 0 || object.equals(fl.object);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    if (height == 0) return object.hashCode();
    else return height;
  }

  @Override
  public String toString() {
    if (height == 0) {
      if (object == null) return "null";
      return object.toString();
    } else {
      if (object != null) return object.toString(); // substitute message
      return (height > 0) ? "top" : "bottom";
    }
  }
  
  public Lattice top() { return topValue; }
  public Lattice bottom() { return bottomValue; }

  public Lattice meet(Lattice other) {
    FlatLattice fl = (FlatLattice)other;
    if (height > fl.height) return other;
    else if (height < fl.height) return this;
    else if (height != 0 || object.equals(fl.object)) return this;
    else return bottomValue;
  }

  public Lattice join(Lattice other) {
    FlatLattice fl = (FlatLattice)other;
    if (height < fl.height) return other;
    else if (height > fl.height) return this;
    else if (height != 0 || object.equals(fl.object)) return this;
    else return topValue;
  }

  public boolean includes(Lattice other) {
    FlatLattice fl = (FlatLattice)other;
    return height > fl.height || equals(other);
  }
  
  /** Return a bottom value with a specific message */
  public static FlatLattice newBottom(Object obj) {
    if (obj == null) return bottomValue;
    return new FlatLattice(-1,obj);
  }

  private static final Vector<FlatLattice> integerValues = new Vector<FlatLattice>();
  private static final Vector<FlatLattice> negIntegerValues = new Vector<FlatLattice>();

  /** Return an integer as a flat lattice value */
  public static FlatLattice newInteger(int i) {
    if (i >= 0) {
      int n = integerValues.size();
      while (i >= n) {
        integerValues.addElement(new FlatLattice(new Integer(n++)));
      }
      return integerValues.elementAt(i);
    } else {
      i = ~i;
      int n = negIntegerValues.size();
      while (i >= n) {
        negIntegerValues.addElement(new FlatLattice(Integer.valueOf(~(n++))));
      }
      return negIntegerValues.elementAt(i);
    }
  }

  /** Return integer lattice value as an integer.
   * Legal only if in the domain and if the value is an integer.
   * @precondition inDomain()
   * @see #inDomain
   */ 
  public int intValue() {
    return ((Integer)getValue()).intValue();
  }
  
  private static final FlatLattice falseValue = new FlatLattice(Boolean.FALSE);
  private static final FlatLattice trueValue = new FlatLattice(Boolean.TRUE);

  /** Return a boolean as a flat lattice value */
  public static FlatLattice newBoolean(boolean b) {
    return b ? trueValue : falseValue;
  }

  /** Return integer lattice value as an integer.
   * Legal only if in the domain and if the value is an integer.
   * @precondition inDomain()
   * @see #inDomain
   */ 
  public boolean booleanValue() {
    return ((Boolean)getValue()).booleanValue();
  }  
}
