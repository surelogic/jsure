package com.surelogic.dropsea.ir.drops.nullable;

import com.surelogic.aast.promise.NullableNode;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.common.XUtil;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;

public final class NullablePromiseDrop extends BooleanPromiseDrop<NullableNode> {

  public NullablePromiseDrop(NullableNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.LOCK_ASSURANCE_CAT);
    if (!XUtil.useExperimental()) {
    setMessage(20, NonNullRules.NULLABLE, DebugUnparser.toString(getNode()));
    }
  }
}
