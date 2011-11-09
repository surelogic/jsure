/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/PossiblyImmutableSet.java,v 1.2 2005/06/30 21:45:40 chance Exp $
 */
package edu.cmu.cs.fluid.util;

import java.util.Set;

/**
 * A set that definitely supports {@link #addCopy} possibly
 * by creating a new set and possibly by mutating itself.
 * @see ImmutableSet
 * @see MutableSet
 * @author boyland
 */
public interface PossiblyImmutableSet<T> extends Set<T> {

  /** Return the set that includes the given element. 
   * It may or may not mutate itself as well.
   */
  public PossiblyImmutableSet<T> addCopy(T element);

}