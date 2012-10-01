package com.surelogic.dropsea.ir.drops;

import com.surelogic.aast.promise.ScopedPromiseNode;
import com.surelogic.common.XUtil;

import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "promise" scoped promises.
 */
public final class PromisePromiseDrop extends ScopedPromiseDrop {

  public PromisePromiseDrop(ScopedPromiseNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.PROMISE_CAT);
    if (!XUtil.useExperimental()) {
    setMessage(19, getAAST());
    }
  }
}