package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.aast.IAASTNode;
import com.surelogic.analysis.MethodCallUtils.EnclosingRefs;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class NeededInstanceLock extends AbstractNeededLock {
  // needs to be visible to HeldInstanceLock
  final IRNode objectRefExpr;
  
  // Must use NeededLockFactory
  NeededInstanceLock(
      final IRNode objectRefExpr, final LockImplementation lockImpl,
      final IRNode source, final Reason reason,
      final PromiseDrop<? extends IAASTNode> assuredPromise, final boolean needsWrite) {
    super(source, reason, assuredPromise, needsWrite, lockImpl);
    this.objectRefExpr = objectRefExpr;
  }

  @Override
  public final NeededLock replaceEnclosingInstanceReference(final EnclosingRefs refs) {
    final IRNode newRef = refs.replace(objectRefExpr);
    if (newRef == null) { // no change
      return this;
    } else {
      return new DerivedNeededInstanceLock(
          this, newRef, lockImpl, source, reason, assuredPromise, needsWrite);
    }
  }
}
