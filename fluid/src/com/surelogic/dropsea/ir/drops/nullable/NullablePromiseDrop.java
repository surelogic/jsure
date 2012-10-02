package com.surelogic.dropsea.ir.drops.nullable;

import com.surelogic.aast.promise.NullableNode;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

public final class NullablePromiseDrop extends BooleanPromiseDrop<NullableNode> {

  public NullablePromiseDrop(NullableNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.LOCK_ASSURANCE_CAT);
  }
}
