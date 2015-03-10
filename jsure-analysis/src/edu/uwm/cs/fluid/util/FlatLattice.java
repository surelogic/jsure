package edu.uwm.cs.fluid.util;

/**
 * @author boyland
 */
public final class FlatLattice extends AbstractLattice<Object> {
  public static final FlatLattice prototype = new FlatLattice();
  
  private final Object topObject = new Object() { @Override public String toString() { return "<top>"; }};
  private final Object bottomObject = new Object(){ @Override public String toString() { return "<bot>"; }};
  
  @Override
  public boolean lessEq(Object v1, Object v2) {
    return v1 == bottomObject || v2 == topObject || equals(v1,v2);
  }

  @Override
  public Object top() {
    return topObject;
  }

  @Override
  public Object bottom() {
    return bottomObject;
  }

  @Override
  public Object join(Object v1, Object v2) {
    if (v1 == bottomObject) return v2;
    if (v2 == bottomObject) return v1;
    if (equals(v1,v2)) {
      return v1;
    }
    return topObject;
  }

  @Override
  public Object meet(Object v1, Object v2) {
    if (v1 == topObject) return v2;
    if (v2 == topObject) return v1;
    if (equals(v1,v2)) {
      return v1;
    }
    return bottomObject;
  }

 }