package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.aast.IAASTNode;
import com.surelogic.analysis.MethodCallUtils.EnclosingRefs;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class NeededFieldRefLock extends AbstractNeededLock {
  // needs to be visible to HeldInstanceLock
  final IRNode objectRefExpr;
  
  /**
   * VariableDeclarator of the field that is being referenced.
   */
  protected final IRNode varDecl;
  
  // Must use NeededLockFactory
  NeededFieldRefLock(
      final IRNode objectRefExpr, final IRNode varDecl, 
      final LockImplementation lockImpl,
      final IRNode source, final Reason reason,
      final PromiseDrop<? extends IAASTNode> assuredPromise, final boolean needsWrite) {
    super(source, reason, assuredPromise, needsWrite, lockImpl);
    this.objectRefExpr = objectRefExpr;
    this.varDecl = varDecl;
  }

  @Override
  public final NeededLock replaceEnclosingInstanceReference(final EnclosingRefs refs) {
    final IRNode newRef = refs.replace(objectRefExpr);
    if (newRef == null) { // no change
      return this;
    } else {
      return new DerivedNeededFieldRefLock(
          this, newRef, varDecl, lockImpl, source, reason, assuredPromise, needsWrite);
    }
  }
}
