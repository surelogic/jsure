package com.surelogic.dropsea.ir.drops.promises;

import com.surelogic.aast.promise.ImmutableRefNode;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;

public final class ImmutableRefPromiseDrop extends BooleanPromiseDrop<ImmutableRefNode> {

  public ImmutableRefPromiseDrop(ImmutableRefNode a) {
    super(a);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
    setMessage(14, DebugUnparser.toString(getNode()));
  }
}
