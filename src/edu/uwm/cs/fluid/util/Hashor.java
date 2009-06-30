package edu.uwm.cs.fluid.util;

/**
 * A collection of functions to be used for hashtables.
 * We need this because equality of (say) arrays isn't defined to do the correct
 * thing, and if we redefine equality, we should redefine hashtables.
 * We would like the equivalent of a {@link java.util.Comparator}.
 * Unfortunately, this isn't standard---we have to
 * roll our own hash tables.
 * @author boyland
 * @param <E> the type of elements to hash.
 * @see HashorMap
 */
public interface Hashor<E> {
  
  public static class Wrapper<T> {
    final Hashor<T> hashor;
    final T value;
    public Wrapper(Hashor<T> h, T v) {
      hashor = h;
      value = v;
    }
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
      if (o instanceof Wrapper) {
        return hashor.equals(value,((Wrapper<T>)o).value);
      }
      return false;
    }
    @Override
    public int hashCode() {
      return hashor.hashCode(value);
    }
  }

  /**
   * Return true if these values are equivalent.
   * @param v1
   * @param v2
   * @return v1 equivalent to v2
   */
  public boolean equals(E v1, E v2);
  
  /**
   * Return a hashcode of the element.
   * @param v
   * @return v's hashcode
   */
  public int hashCode(E v);
  
}
