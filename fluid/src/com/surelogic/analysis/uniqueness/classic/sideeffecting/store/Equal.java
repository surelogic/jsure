package com.surelogic.analysis.uniqueness.classic.sideeffecting.store;

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
    if (node.contains(v1)) {
      if (node.contains(v2)) return both;
      else return !both;
    } else {
      if (node.contains(v2)) return !both;
      else return true; // error fixed 2011/6/24: was "both".
    }
  }
}
