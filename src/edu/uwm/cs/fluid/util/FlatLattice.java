package edu.uwm.cs.fluid.util;

/**
 * @author boyland
 */
public class FlatLattice extends AbstractLattice<Object> {
  public static final FlatLattice prototype = new FlatLattice();
  
  private final Object topObject = new Object() { @Override public String toString() { return "<top>"; }};
  private final Object bottomObject = new Object(){ @Override public String toString() { return "<bot>"; }};
  
  public boolean lessEq(Object v1, Object v2) {
    return v1 == bottomObject || v2 == topObject || equals(v1,v2);
  }

  public Object top() {
    return topObject;
  }

  public Object bottom() {
    return bottomObject;
  }

  public Object join(Object v1, Object v2) {
    if (v1 == bottomObject) return v2;
    if (v2 == bottomObject) return v1;
    if (equals(v1,v2)) {
      return v1;
    }
    return topObject;
  }

  public Object meet(Object v1, Object v2) {
    if (v1 == topObject) return v2;
    if (v2 == topObject) return v1;
    if (equals(v1,v2)) {
      return v1;
    }
    return bottomObject;
  }

 }