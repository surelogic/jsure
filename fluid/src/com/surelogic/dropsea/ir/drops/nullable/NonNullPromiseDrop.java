package com.surelogic.dropsea.ir.drops.nullable;

import com.surelogic.aast.promise.NonNullNode;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;

public final class NonNullPromiseDrop extends BooleanPromiseDrop<NonNullNode> {

  public NonNullPromiseDrop(NonNullNode a) {
    super(a);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
    setMessage(20, NonNullRules.NONNULL, DebugUnparser.toString(getNode()));
  }
}
