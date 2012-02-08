package com.surelogic.analysis.uniqueness.classic.sideeffecting.store;

import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

final class Add implements Apply {
  private final Object var;
  private final ImmutableHashOrderSet<Object> additional;

  public Add(final Object v, final ImmutableHashOrderSet<Object> add) {
    var = v;
    additional = add;
  }

  public ImmutableHashOrderSet<Object> apply(
      final ImmutableHashOrderSet<Object> other) {
    if (other.contains(var)) {
      return other.union(additional);
    }
    return other;
  }
}
