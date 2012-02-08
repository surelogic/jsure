package com.surelogic.analysis.uniqueness.plusFrom.traditional.store;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.Triple;

/**
 * Class used mainly as a "type def"
 */
public final class FieldTriple extends Triple<ImmutableHashOrderSet<Object>, IRNode, ImmutableHashOrderSet<Object>> {
  public FieldTriple(final ImmutableHashOrderSet<Object> o1,
      final IRNode o2, final ImmutableHashOrderSet<Object> o3) {
    super(o1, o2, o3);
  }
}
