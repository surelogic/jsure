package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.ImmutableRefNode;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;

public final class ImmutableRefPromiseDrop extends BooleanPromiseDrop<ImmutableRefNode> {

  public ImmutableRefPromiseDrop(ImmutableRefNode a) {
    super(a);
    setCategorizingString(JavaGlobals.LOCK_ASSURANCE_CAT);
    setMessage(14, DebugUnparser.toString(getNode()));
  }
}
