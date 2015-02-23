/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/MutableSet.java,v 1.2 2005/06/30 21:45:40 chance Exp $
 */
package edu.cmu.cs.fluid.util;

import java.util.Collection;
import java.util.HashSet;


/**
 * A mutable set implementation directly based on hash sets.
 * This implementation supports {@link addCopy} by mutating itself.
 * @author boyland
 */
public class MutableSet<T> extends HashSet<T> implements PossiblyImmutableSet<T> {
  public MutableSet() {
    super();
  }

  public MutableSet(int initialCapacity) {
    super(initialCapacity);
  }

  public MutableSet(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public MutableSet(Collection<T> c) {
    super(c);
  }

  @SuppressWarnings("unchecked")
  public PossiblyImmutableSet<T> addCopy(T x) {
    add(x);
    return this;
  }
}
