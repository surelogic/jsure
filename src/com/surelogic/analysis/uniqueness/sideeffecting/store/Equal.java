package com.surelogic.analysis.uniqueness.sideeffecting.store;

import edu.cmu.cs.fluid.util.ImmutableSet;

final class Equal implements Filter {
  private final Object v1, v2;
  private final boolean both;

  public Equal(final Object x1, final Object x2, final boolean areEqual) {
    v1 = x1;
    v2 = x2;
    both = areEqual;
  }

  public boolean filter(ImmutableSet<Object> node) {
    return (node.contains(v1) == node.contains(v2)) == both;
  }
}
