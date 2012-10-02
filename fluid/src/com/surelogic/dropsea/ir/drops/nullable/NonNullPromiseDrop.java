package com.surelogic.dropsea.ir.drops.nullable;

import com.surelogic.aast.promise.NonNullNode;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

public final class NonNullPromiseDrop extends BooleanPromiseDrop<NonNullNode> {

  public NonNullPromiseDrop(NonNullNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.LOCK_ASSURANCE_CAT);
  }
}
