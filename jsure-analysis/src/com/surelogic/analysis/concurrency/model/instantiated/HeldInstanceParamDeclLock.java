package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.RequiresLockPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.parse.JJNode;

/**
 * This is used for the case of "p:Lock" appearing in a RequiresLock or
 * ReturnsLock annotation.  In this case the variable use 'p' is resolved
 * to the ParameterDeclaration node for 'p'.  But when 'p' is used in the code
 * body (say in a return statement or lock expression) its VariableUseExpression
 * appears as the objectRefExpr in a HeldInstanceLock object.  The HeldLock
 * originating from the method body will always be the argument to
 * mustALias() or mustSatisfy() of the HeldInstanceParamDeclLock object.  So 
 * we have to deal with pretending to be a variable use expression.
 */
public final class HeldInstanceParamDeclLock extends AbstractHeldLock {
  private final IRNode paramDecl;
  
  // Must use the HeldLockFactory
  HeldInstanceParamDeclLock(
      final IRNode paramDecl, final LockImplementation lockImpl,
      final IRNode source, final Reason reason, final boolean needsWrite,
      final PromiseDrop<?> lockPromise,
      final RequiresLockPromiseDrop supportingDrop) {
    super(source, reason, needsWrite, lockImpl, lockPromise, supportingDrop);
    this.paramDecl = paramDecl;
  }

  /**
   * Check that the same lock is used, and then use syntactic equality of the
   * object expressions.
   */
  @Override
  public boolean mustAlias(final HeldLock lock, final ThisExpressionBinder teb) {
    if (this == lock) {
      return true;
    } else if (lock instanceof HeldInstanceParamDeclLock) {
      final HeldInstanceParamDeclLock o = (HeldInstanceParamDeclLock) lock;
      return (holdsWrite == o.holdsWrite) 
          && lockImpl.equals(o.lockImpl)
          && paramDecl.equals(o.paramDecl);
    } else if (lock instanceof HeldInstanceLock) {
      /* The other lock must be a HeldInstanceLock whose objectRef expression
       * is a variable use expression that binds to paramDecl.
       */
      final HeldInstanceLock o = (HeldInstanceLock) lock;
      return (holdsWrite == o.holdsWrite)
          && lockImpl.equals(o.lockImpl)
          && VariableUseExpression.prototype.includes(
              JJNode.tree.getOperator(o.objectRefExpr))
          && paramDecl.equals(teb.getBinding(o.objectRefExpr));
    } else {
      return false;
    }
  }
  
  @Override
  public boolean mustSatisfy(final NeededLock lock, final ThisExpressionBinder teb) {
    if (lock instanceof NeededInstanceLock) {
      final NeededInstanceLock o = (NeededInstanceLock) lock;
      return (holdsWrite || (!holdsWrite && !o.needsWrite()))
          && lockImpl.equals(o.lockImpl)
          && VariableUseExpression.prototype.includes(
              JJNode.tree.getOperator(o.objectRefExpr))
          && paramDecl.equals(teb.getBinding(o.objectRefExpr));
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    int result = 29; // Don't get confused with HeldInstanceLocks
    result += 31 * reason.hashCode();
    result += 31 * (holdsWrite ? 1 : 0);
    result += 31 * lockImpl.hashCode();
    result += 31 * source.hashCode();
    result += 31 * paramDecl.hashCode();
    result += 31 * ((lockPromise == null) ? 0 : lockPromise.hashCode());
    result += 31 * ((supportingDrop == null) ? 0 : supportingDrop.hashCode());
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) { 
      return true;
    } else if (other instanceof HeldInstanceParamDeclLock) {
      final HeldInstanceParamDeclLock o = (HeldInstanceParamDeclLock) other;
      return this.reason == o.reason &&
          this.holdsWrite == o.holdsWrite &&
          this.lockImpl.equals(o.lockImpl) && 
          this.paramDecl.equals(o.paramDecl) &&
          this.source.equals(o.source) &&
          (this.lockPromise == null ? o.lockPromise == null : this.lockPromise.equals(o.lockPromise)) && 
          (this.supportingDrop == null ? o.supportingDrop == null : this.supportingDrop.equals(o.supportingDrop));
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "<" + ParameterDeclaration.getId(paramDecl) +
        lockImpl.getPostfixId() + ">." + 
        (holdsWrite ? "write" : "read");
  }
}
