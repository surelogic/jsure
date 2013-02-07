package edu.uwm.cs.fluid.util;

import edu.cmu.cs.fluid.util.ArrayIterator;

/**
 * An array lattice where each array element is associated with a index value.
 * @param <K> The type of the associative index values
 * @param <L> The lattice of the array elements
 * @param <T> the type of the array elements
 */
public abstract class AssociativeArrayLattice<K, L extends Lattice<T>, T> extends ArrayLattice<L, T> {
  /** The array of associative indices. */
  protected final K[] indices;

  
  
  protected AssociativeArrayLattice(
      final L base, final K[] keys) {
    super(base, keys.length);
    indices = keys;
  }
  
  
  
  // ======================================================================
  // == New methods for associative arrays
  // ======================================================================
  
  public final T[] replaceValue(final T[] array, final K key, final T newValue) {
    final int index = indexOf(key);
    if (index != -1) {
      return replaceValue(array, index, newValue);
    } else {
      return array;
    }
  }
  
  public final K getKey(final int i) {
    return indices[i];
  }
  
  /**
   * Search associative values and return the array index of the given value.
   * 
   * @param key
   *          The declaration to look for.
   * @return The index of the declaration or <code>-1</code> if the key is not
   *         found.
   */
  public final int indexOf(final K key) {
    for (int i = 0; i < indices.length; i++) {
      if (indexEquals(key, indices[i])) return i;      
    }
    return -1;
  }
  
  /**
   * Get an iterator over the indices.
   */
  public final Iterable<K> indices() {
    return new ArrayIterator<K>(indices);
  }
  
  /**
   * Compare two index values.  Used to match index values during index lookup.
   */
  protected abstract boolean indexEquals(K k1, K k2);
  
  /**
   * Get the empty value of the lattice.  This is usually a slightly modified
   * version of the bottom value so that errors can be distinguished from
   * legitimate values.
   */
  public abstract T[] getEmptyValue();
  
//  /**
//   * Is the lattice value normal: that is, neither top nor bottom, or erroneous.
//   */
//  public abstract boolean isNormal(T[] value);
  


  // ======================================================================
  // == From regular array
  // ======================================================================
  
  @Override
  protected final void indexToString(final StringBuilder sb, final int i) {
    indexToString(sb, indices[i]);
  }
  
  protected abstract void indexToString(StringBuilder sb, K index);
}
