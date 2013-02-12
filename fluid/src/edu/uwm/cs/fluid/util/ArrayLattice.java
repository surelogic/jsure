package edu.uwm.cs.fluid.util;

/**
 * A lattice in which the elements are fixed size arrays, each element of which is
 * a lattice element in its own right.  This lattice is cached, but it's
 * not clear that this is useful except for top and bottom.  Experience is needed
 * so we can evaluate.
 * <p>
 * This class takes the lattice type as well as the underlying
 * type as a parameter.  This only helps {@link #getBaseLattice()} have
 * a more precise type.  If this precision is not needed, then {@link L} can
 * always simply be <code>Lattice&lt;T&gt;</code>.
 * @param <L> the lattice of the array elements.
 * @param <T> the type of the array elements
 */
public abstract class ArrayLattice<L extends Lattice<T>, T> extends CachingLattice<T[]> {
  protected final L baseLattice;
  protected final int size;
  
  /**
   * Create an array lattice using the given base lattice and size.
   * @param base base lattice for elements in the arrays.
   * @param n length of the arrays
   */
  public ArrayLattice(L base, int n) {
    baseLattice = base;
    size = n;
  }
  
  public final L getBaseLattice() {
    return baseLattice;
  }
  
  public final int getSize() {
    return size;
  }
  
  @Override
  public final boolean lessEq(T[] v1, T[] v2) {
    for (int i=0; i < size; ++i) {
      if (!baseLattice.lessEq(v1[i],v2[i])) return false;
    }
    return true;
  }
  
  @Override
  public final boolean equals(T[] v1, T[] v2) {
    for (int i=0; i < size; ++i) {
      if (!baseLattice.equals(v1[i],v2[i])) return false;
    }
    return true;
  }
  
  @Override
  public final int hashCode(T[] v) {
    // we use the requirement for list hashcodes.
    int h = 1;
    for (int i=0; i < size; ++i) {
     h = 31 * h + baseLattice.hashCode(v[i]);
    }
    return h;
  }
  
  /**
   * Return a new array with the given element changed.
   * This method does not cache the result!
   * Normally this should only be used in intermediate computations.
   * <p>
   * XXX: I'm not sure why this function is public.
   * @param array
   * @param i
   * @param newValue
   * @return new (uncached) array
   * @see #replaceValue(Object[], int, Object)
   */
  public final T[] set(T[] array, int i, T newValue) {
    if (baseLattice.equals(array[i],newValue)) return array;
    T[] result = array.clone();
    result[i] = newValue;
    return result;
  }
  
  /**
   * Return an array with the given element changed.
   * The result is a cached array.
   * @param array
   * @param i
   * @param newValue
   * @return
   */
  public final T[] replaceValue(T[] array, int i, T newValue) {
    return cache(set(array,i,newValue));
  }

  protected abstract T[] newArray();
  
  @Override
  protected final T[] computeTop() {
    final T[] topArray = newArray();
    final T top = baseLattice.top();
    for (int i = 0; i < size; i++) topArray[i] = top;
    return topArray;
  }

  @Override
  protected final T[] computeBottom() {
    final T[] bottomArray = newArray();
    final T bottom = baseLattice.bottom();
    for (int i = 0; i < size; i++) bottomArray[i] = bottom;
    return bottomArray;
  }

  @Override
  protected final T[] computeMeet(T[] v1, T[] v2) {
    T[] result = v1.clone();
    for (int i=0; i < size; ++i) {
      result[i] = baseLattice.meet(v1[i],v2[i]);
    }
    return result;
  }

  @Override
  protected final T[] computeJoin(T[] v1, T[] v2) {
    T[] result = v1.clone();
    for (int i=0; i < size; ++i) {
      result[i] = baseLattice.join(v1[i],v2[i]);
    }
    return result;
  }

  @Override
  protected final T[] computeWiden(T[] v1, T[] v2) {
    T[] result = v1.clone();
    for (int i=0; i < size; ++i) {
      result[i] = baseLattice.widen(v1[i],v2[i]);
    }
    return result;   
  }

  @Override
  public final String toString(final T[] v) {
    final StringBuilder sb = new StringBuilder();
    if (v == top()) sb.append("** TOP **").append(toStringPrefixSeparator());
    else if (v == bottom()) sb.append("** BOTTOM **").append(toStringPrefixSeparator());
    
    sb.append(toStringOpen());
    for (int i = 0; i < v.length; ++i) {
      if (i != 0) sb.append(toStringSeparator());
      indexToString(sb, i);
      sb.append(toStringConnector());
      valueToString(sb, v[i]);
    }
    sb.append(toStringClose());
    return sb.toString();
  }
  
  protected String toStringPrefixSeparator() { return " "; }
  protected String toStringOpen() { return "["; }
  protected String toStringSeparator() { return ", "; }
  protected String toStringConnector() { return "->"; }
  protected String toStringClose() { return "]"; }
  
  protected void indexToString(final StringBuilder sb, final int i) {
    sb.append(Integer.toString(i));
  }
  
  protected void valueToString(final StringBuilder sb, final T v) {
    sb.append(baseLattice.toString(v));
  }
}
