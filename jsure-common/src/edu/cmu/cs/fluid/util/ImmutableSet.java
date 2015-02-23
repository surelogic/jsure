package edu.cmu.cs.fluid.util;

import java.util.Set;

/** A set with fixed membership: a mathematical value.
 * This interface includes support for unions, intersections
 * differences and (optionally) inverses.
 * An inverse of a finite set is always infinite.
 * Request an iterator of an infinite set, or
 * asking the elements to be listed in an array will not
 * give useful results.
 */
public interface ImmutableSet<T> extends Set<T>, PossiblyImmutableSet<T> {
  @SuppressWarnings("rawtypes")
  ImmutableSet[] NO_SETS = new ImmutableSet[0];
               
  /** Returns true if the set is infinite.
   * In this case, it is usually unwise to ask for an iteration
   * of all the elements or to ask for an array
   * of all of the elements.
   */
  public boolean isInfinite();

  /** Return a new set that has this additional element added.
   * This method never mutates this, and always returns something that
   * implements {@link ImmutableSet}.  In Java 1.5, we will be able to indicate
   * this in the return type.
   * @return new set with additional element (or same if this element already included.)
   */
  @Override
  public ImmutableSet<T> addCopy(T element);
  
  /** Return the inverse of this set: which includes
   * everything this set does not include.
   * @exception UnsupportedOperationException
   * if this set does not support the inversion operation.
   */
  public ImmutableSet<T> invertCopy() throws UnsupportedOperationException;

  /** Return the set that does not include the given element. */
  public ImmutableSet<T> removeCopy(T element);

  /** Return the mathematical union of two sets. */
  public ImmutableSet<T> union(Set<T> other);

  /** Return the mathematical union of two sets. */
  public ImmutableSet<T> intersection(Set<T> other);

  /** Return the mathematical difference of two sets:
   * The result has all the elements of this set that
   * are not contained in the argument set.
   */
  public ImmutableSet<T> difference(Set<T> other);

}
