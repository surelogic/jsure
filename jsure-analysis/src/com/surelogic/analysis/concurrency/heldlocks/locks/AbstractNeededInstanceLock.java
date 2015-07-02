package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;

abstract class AbstractNeededInstanceLock extends AbstractNeededLock {
  /**
	 * The object-valued expression that this lock is associated with.
	 */
  final IRNode obj;

  AbstractNeededInstanceLock(final IRNode o, final LockModel lm, final Type type) {
    super(lm, type);
    obj = o;
  }

  abstract boolean satisfiesAAST(
      AASTHeldInstanceLock lock, ThisExpressionBinder teb);

  abstract boolean satisfiesIR(
      IRHeldInstanceLock lock, ThisExpressionBinder teb);

  abstract boolean satisfiesFieldRef(
      HeldFieldRefLock lock, ThisExpressionBinder teb);
  
  @Override
  public boolean mayHaveAliasInCallingContext() {
    return QualifiedReceiverDeclaration.prototype.includes(obj);
  }
}
