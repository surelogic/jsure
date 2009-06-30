package edu.uwm.cs.fluid.util;

import java.util.ArrayList;
import java.util.List;

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
 * @author boyland
 * @param <L> the lattice of the array elements.
 * @param <T> the type of the array elements
 */
public class ArrayLattice<L extends Lattice<T>, T> extends CachingLattice<T[]> {

  private final L baseLattice;
  private final T[] prototype;
  private final int size;
  
  /**
   * Create an array lattice using the given base lattice and size.
   * @param base base lattice for elements in the arrays.
   * We take a prototype array so that we can create arrays of the correct
   * type by cloning.  That is because <code>new T[n]</code> doesn't 
   * work in Java (sigh).
   * @param n length of the arrays
   * @param p sample value of type T[] (recommended: size = 0)
   */
  public ArrayLattice(L base, int n, T[] p) {
    baseLattice = base;
    size = n;
    prototype = p;
  }
  
  public L getBaseLattice() {
    return baseLattice;
  }
  
  public boolean lessEq(T[] v1, T[] v2) {
    for (int i=0; i < size; ++i) {
      if (!baseLattice.lessEq(v1[i],v2[i])) return false;
    }
    return true;
  }
  
  @Override
  public boolean equals(T[] v1, T[] v2) {
    for (int i=0; i < size; ++i) {
      if (!baseLattice.equals(v1[i],v2[i])) return false;
    }
    return true;
  }
  
  @Override
  public int hashCode(T[] v) {
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
  public T[] set(T[] array, int i, T newValue) {
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
  public T[] replaceValue(T[] array, int i, T newValue) {
    return cache(set(array,i,newValue));
  }

  protected T[] makeArray(List<T> l) {
    T[] a = size <= prototype.length ? prototype.clone() : prototype;
    return l.toArray(a);
  }

  @Override
  protected T[] computeTop() {
    T top = baseLattice.top();
    List<T> tops = new ArrayList<T>(size);
    for (int i=0; i < size; ++i) {
      tops.add(top);
    }
    return makeArray(tops);
  }

  @Override
  protected T[] computeBottom() {
    T bot = baseLattice.bottom();
    List<T> bots = new ArrayList<T>(size);
    for (int i=0; i < size; ++i) {
      bots.add(bot);
    }
    return makeArray(bots);
  }

  @Override
  protected T[] computeMeet(T[] v1, T[] v2) {
    T[] result = v1.clone();
    for (int i=0; i < size; ++i) {
      result[i] = baseLattice.meet(v1[i],v2[i]);
    }
    return result;
  }

  @Override
  protected T[] computeJoin(T[] v1, T[] v2) {
    T[] result = v1.clone();
    for (int i=0; i < size; ++i) {
      result[i] = baseLattice.join(v1[i],v2[i]);
    }
    return result;
  }

  @Override
  protected T[] computeWiden(T[] v1, T[] v2) {
    T[] result = v1.clone();
    for (int i=0; i < size; ++i) {
      result[i] = baseLattice.widen(v1[i],v2[i]);
    }
    return result;   
  }

  @Override
  public String toString(T[] v) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < v.length; ++i) {
      if (i == 0) sb.append('[');
      else sb.append(',');
      sb.append(baseLattice.toString(v[i]));
    }
    sb.append(']');
    return sb.toString();
  }
  
}