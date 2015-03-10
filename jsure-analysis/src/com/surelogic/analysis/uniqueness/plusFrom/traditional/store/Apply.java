package com.surelogic.analysis.uniqueness.plusFrom.traditional.store;

import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

interface Apply {
  public ImmutableHashOrderSet<Object> apply(ImmutableHashOrderSet<Object> other);
}
