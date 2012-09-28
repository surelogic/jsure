package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.NotThreadSafeNode;
import com.surelogic.common.XUtil;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

/**
 * Promise drop for "NotThreadSafe" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class NotThreadSafePromiseDrop extends BooleanPromiseDrop<NotThreadSafeNode> {

  public NotThreadSafePromiseDrop(NotThreadSafeNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.LOCK_ASSURANCE_CAT);
    final String name = XUtil.useExperimental() ? JavaNames.getRelativeTypeName(getNode()) : JavaNames.getTypeName(getNode());
    setMessage(Messages.LockAnnotation_notThreadSafeDrop, name);
  }
}