package com.surelogic.analysis.uniqueness.classic.sideeffecting.store;

import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

final class Remove implements Apply {
  private final ImmutableHashOrderSet<Object> old;

  public Remove(final ImmutableHashOrderSet<Object> rid) {
    old = rid;
  }

  @Override
  public ImmutableHashOrderSet<Object> apply(
      final ImmutableHashOrderSet<Object> other) {
    return other.difference(old);
  }
}
