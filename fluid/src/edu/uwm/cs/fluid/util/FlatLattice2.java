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
    
    // Force equals, toString, and hashCode to be overridden
    @Override
    public abstract String toString();
    
    @Override
    public abstract boolean equals(Object other);

    @Override
    public abstract int hashCode();
  }
  
  private final static class Bottom<T> extends Element<T> {
    public Bottom() {
      super(-1);
    }
    
    @Override
    public String toString() {
      return "<bot>";
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
    private final String msg;
    
    public Top(final String m) {
      super(1);
      msg = m;
    }
    
    public Top() {
      this("<top>");
    }
    
    @Override
    public String toString() {
      return msg;
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
    if (v1.height != v2.height) return false;
    return v1.height != 0 || v1.getValue().equals(v2.getValue());
  }
  
  public boolean lessEq(final Element<T> v1, final Element<T> v2) {
    if (v1.height == -1 || v2.height == 1) {
      //     v1 is BOTTOM, always less than any v2
      // or  v2 is TOP, always greater than any v1
      return true;
    } else if (v1.height == v2.height) {
      if (v1.height == 0) {
        // Compare the elements
        return v1.getValue().equals(v2.getValue());
      } else {
        // TOP <= TOP, BOTTOM <= BOTTOM
        return true;
      }
    } else {
      return false;
    }
  }

  public Element<T> top() {
    return topObject;
  }

  public Element<T> bottom() {
    return bottomObject;
  }
  
  public static <T> Element<T> errorTop(final String msg) {
    return new Top<T>(msg);
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