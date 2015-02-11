package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.MutableNode;
import com.surelogic.dropsea.ir.drops.ModifiedBooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "Mutable" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class MutablePromiseDrop extends ModifiedBooleanPromiseDrop<MutableNode> {

  public MutablePromiseDrop(MutableNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.LOCK_ASSURANCE_CAT);
  }

  @Override
  public boolean isIntendedToBeCheckedByAnalysis() {
    return false;
  }
}