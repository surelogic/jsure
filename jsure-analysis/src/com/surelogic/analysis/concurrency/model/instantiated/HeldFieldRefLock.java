package com.surelogic.analysis.concurrency.model.instantiated;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.concurrency.model.SyntacticEquality;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.RequiresLockPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;

public final class HeldFieldRefLock extends AbstractHeldLock {
  // default visibility so HeldInstaceParameterDeclLock can use it
  final IRNode objectRefExpr;
  
  /**
   * VariableDeclarator of the field that is being referenced.
   */
  final IRNode varDecl;

  // Must use the HeldLockFactory
  HeldFieldRefLock(
      final IRNode objectRefExpr, final IRNode varDecl, final LockImplementation lockImpl,
      final IRNode source, final Reason reason, final boolean needsWrite,
      final PromiseDrop<?> lockPromise,
      final RequiresLockPromiseDrop supportingDrop) {
    super(source, reason, needsWrite, lockImpl, lockPromise, supportingDrop);
    this.objectRefExpr = objectRefExpr;
    this.varDecl = varDecl;
  }

  /**
   * Check that the same lock is used, and then use syntactic equality of the
   * object expressions.
   */
  @Override
  public boolean mustAlias(final HeldLock lock, final ThisExpressionBinder teb) {
    if (this == lock) {
      return true;
    } else if (lock instanceof HeldFieldRefLock) {
      final HeldFieldRefLock o = (HeldFieldRefLock) lock;
      return (holdsWrite == o.holdsWrite) && lockImpl.equals(o.lockImpl) &&
          varDecl.equals(o.varDecl) && 
          SyntacticEquality.checkSyntacticEquality(objectRefExpr, o.objectRefExpr, teb);
    } else if (lock instanceof HeldInstanceLock) {
      // Only proceed if the other lock has a fieldRef expression for its object expr
      final HeldInstanceLock o = (HeldInstanceLock) lock;
      if (FieldRef.prototype.includes(o.objectRefExpr)) {
        final IRNode objectRef2 = teb.bindThisExpression(FieldRef.getObject(o.objectRefExpr));
        final IRNode varDecl2 = teb.getBinding(o.objectRefExpr);
        return (holdsWrite == o.holdsWrite) && lockImpl.equals(o.lockImpl) &&
            varDecl.equals(varDecl2) && 
            SyntacticEquality.checkSyntacticEquality(objectRefExpr, objectRef2, teb);
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
  
  @Override
  public boolean mustSatisfy(final NeededLock lock, final ThisExpressionBinder teb) {
    if (lock instanceof NeededFieldRefLock) {
      final NeededFieldRefLock o = (NeededFieldRefLock) lock;
      return (holdsWrite || (!holdsWrite && !o.needsWrite())) &&
          lockImpl.equals(o.lockImpl) &&
          varDecl.equals(o.varDecl) &&
          SyntacticEquality.checkSyntacticEquality(objectRefExpr, o.objectRefExpr, teb);
    } else if (lock instanceof NeededInstanceLock) {
      // Only proceed if the other lock has a fieldRef expression for its object expr
      final NeededInstanceLock o = (NeededInstanceLock) lock;
      if (FieldRef.prototype.includes(o.objectRefExpr)) {
        final IRNode objectRef2 = teb.bindThisExpression(FieldRef.getObject(o.objectRefExpr));
        final IRNode varDecl2 = teb.getBinding(o.objectRefExpr);
        return (holdsWrite || (!holdsWrite && !o.needsWrite())) &&
            lockImpl.equals(o.lockImpl) &&
            varDecl.equals(varDecl2) &&
            SyntacticEquality.checkSyntacticEquality(objectRefExpr, objectRef2, teb);
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * reason.hashCode();
    result += 31 * (holdsWrite ? 1 : 0);
    result += 31 * lockImpl.hashCode();
    result += 31 * source.hashCode();
    result += 31 * objectRefExpr.hashCode();
    result += 31 * varDecl.hashCode();
    result += 31 * ((lockPromise == null) ? 0 : lockPromise.hashCode());
    result += 31 * ((supportingDrop == null) ? 0 : supportingDrop.hashCode());
    return result;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) { 
      return true;
    } else if (other instanceof HeldFieldRefLock) {
      final HeldFieldRefLock o = (HeldFieldRefLock) other;
      return this.reason == o.reason &&
          this.holdsWrite == o.holdsWrite &&
          this.lockImpl.equals(o.lockImpl) && 
          this.objectRefExpr.equals(o.objectRefExpr) &&
          this.varDecl.equals(o.varDecl) &&
          this.source.equals(o.source) &&
          (this.lockPromise == null ? o.lockPromise == null : this.lockPromise.equals(o.lockPromise)) && 
          (this.supportingDrop == null ? o.supportingDrop == null : this.supportingDrop.equals(o.supportingDrop));
    } else {
      return false;
    }
  }
  
  @Override
  public String toString() {
    return "<" + DebugUnparser.toString(objectRefExpr) +
        "." + VariableDeclarator.getId(varDecl) +
        lockImpl.getPostfixId() + ">." + 
        (holdsWrite ? "write" : "read");
  }
}
