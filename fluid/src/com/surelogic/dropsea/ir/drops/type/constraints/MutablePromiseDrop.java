package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.*;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "Mutable" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class MutablePromiseDrop extends BooleanPromiseDrop<MutableNode> {

  public MutablePromiseDrop(MutableNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.LOCK_ASSURANCE_CAT);
  }
}