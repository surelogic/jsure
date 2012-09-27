package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.*;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

/**
 * Promise drop for "Mutable" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class MutablePromiseDrop extends BooleanPromiseDrop<MutableNode> {

  public MutablePromiseDrop(MutableNode a) {
    super(a);
    setCategorizingString(JavaGlobals.LOCK_ASSURANCE_CAT);
    final String name = JavaNames.getTypeName(getNode());
    setMessage(Messages.LockAnnotation_mutableDrop, name);
  }
}