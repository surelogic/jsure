package edu.uwm.cs.fluid.util;

/**
 * An implementation of a flat lattice that uses wrappers around the elements
 * so that we can 
 * <ul>
 * <li>enforce a type on the elements of the lattice and still distinguish top and bottom elements.
 * <li>have bottom elements that hold error messages.
 * </ul>
 */
public final class FlatLattice2<T> extends AbstractLattice<FlatLattice2.Element<T>> {
  public abstract static class Element<T> {
    private final int height;
    
    public Element(final int h) {
      height = h;
    }
    
    public final boolean inDomain() {
      return height == 0;
    }
    
    public T getValue() {
      throw new UnsupportedOperationException();
    }
  }
  
  private final static class Bottom<T> extends Element<T> {
    private final String msg;
    
    public Bottom(final String m) {
      super(-1);
      msg = m;
    }
    
    public Bottom() {
      this("<bot>");
    }
    
    @Override
    public String toString() {
      return msg;
    }
    
    @Override 
    public int hashCode() {
      return -1;
    }
    
    @Override
    public boolean equals(final Object o) {
      return (o instanceof Bottom);
    }
  }
  
  private final static class Top<T> extends Element<T> {
    public Top() {
      super(1);
    }
    
    @Override
    public String toString() {
      return "<top>";
    }
    
    @Override 
    public int hashCode() {
      return 1;
    }
    
    @Override
    public boolean equals(final Object o) {
      return (o instanceof Top);
    }
  }
  
  private final static class Middle<T> extends Element<T> {
    private final T value;
    
    public Middle(final T v) {
      super(0);
      value = v;
    }
    
    @Override
    public T getValue() { 
      return value;
    }
    
    @Override
    public String toString() {
      return value.toString();
    }
    
    @Override 
    public int hashCode() {
      return value.hashCode();
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(final Object o) {
      return (o instanceof Middle) && 
        this.value.equals(((Middle) o).value);
    }
  }

  
  
  private final Element<T> topObject = new Top<T>();
  private final Element<T> bottomObject = new Bottom<T>();
  
  
  
  public FlatLattice2() {
    // do nothing special;
  }
  
  
  
  /**
   * Inject an object into the domain of this lattice.
   */
  public static <T> Element<T> asMember(final T v) {
    return new Middle<T>(v);
  }
  
  
  
  @Override
  public boolean equals(final Element<T> v1, final Element<T> v2) {
//    if (v1 == null) {
//      return v2 == null;
//    } else {
      if (v1.height != v2.height) return false;
      return v1.height != 0 || v1.getValue().equals(v2.getValue());
//    }
  }
  
  public boolean lessEq(final Element<T> v1, final Element<T> v2) {
    return v1.height == -1 || v2.height == 1 || v1.getValue().equals(v2.getValue());
  }

  public Element<T> top() {
    return topObject;
  }

  public Element<T> bottom() {
    return bottomObject;
  }
  
  // XXX: Should this be errorTop instead???
  public static <T> Element<T> errorBottom(final String msg) {
    return new Bottom<T>(msg);
  }

  public Element<T> join(final Element<T> v1, final Element<T> v2) {
    if (v1.height == -1) return v2;
    if (v2.height == -1) return v1;
    if (equals(v1,v2)) {
      return v1;
    }
    return topObject;
  }

  public Element<T> meet(final Element<T> v1, final Element<T> v2) {
    if (v1.height == 1) return v2;
    if (v2.height == 1) return v1;
    if (equals(v1,v2)) {
      return v1;
    }
    return bottomObject;
  }
}