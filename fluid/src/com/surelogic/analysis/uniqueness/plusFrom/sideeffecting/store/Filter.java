package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store;

import edu.cmu.cs.fluid.util.ImmutableSet;

interface Filter {
  public boolean filter(ImmutableSet<Object> other);
}
