package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;

abstract class HeldInstanceLock extends AbstractHeldLock {
  HeldInstanceLock(
      final LockModel lm, final IRNode src, final PromiseDrop<?> sd,
      final boolean assumed, final Type type) {
    super(lm, src, sd, assumed, type);
  }
  
  /**
	 * Here we cop out and use syntactic equality of final expressions instead
	 * of dealing with aliasing.
	 */
  @Override
  public boolean mustAlias(
      final HeldLock lock, final ThisExpressionBinder teb, final IBinder binder) {
    /* First check that the lock name are equal.
     */
    if (!(lock instanceof HeldInstanceLock)) {
      return false;
    } else {
      HeldInstanceLock hil = (HeldInstanceLock) lock;    
      if (getUniqueIdentifier().equals(hil.getUniqueIdentifier())) {
        final IRNode f = this.getFieldOfThis(binder);
        if (f != null && f.equals(hil.getFieldOfThis(binder))) {
          return true;
        }
        if (checkSyntacticEquality(teb, binder, hil)) {
          return isWrite() == lock.isWrite();
        }
      }
      return false;
    }
  }

  @Override
  public boolean mustSatisfy(
      final NeededLock lock, final ThisExpressionBinder teb, final IBinder binder) {
    /* First check that the lock name are equal.
     */
    if (!(lock instanceof NeededInstanceLock)) {
      return false;
    } else {
      final NeededInstanceLock neededLock = (NeededInstanceLock) lock;
      if (getUniqueIdentifier().equals(lock.getUniqueIdentifier())) {
        if (testFieldSpecialCase(lock, teb,binder)) {
          return true;
        }
        if (VariableDeclarator.prototype.includes(neededLock.getObject())) {
          /* Object expression of the held lock must be of the form "this.f"
           * where "f" is the field in the VariableDeclarator.
           */
          return isFieldExprOfThis(binder, neededLock.getObject());
        } else {
          final boolean isSyntacticallyEqual = checkSyntacticEquality(
              teb, binder, neededLock.getObject());
          if (isSyntacticallyEqual) {
            return isWrite() || (!isWrite() && !lock.isWrite());
          }
        }
      }
      return false;
    }
  }
  
  @Override
  public boolean equals(final Object o) {
    if (o instanceof HeldInstanceLock) {
      final HeldInstanceLock hil = (HeldInstanceLock) o;
      return baseEquals(hil) && getObject().equals(hil.getObject());
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append('<');
    sb.append(objToString());
    sb.append(">:");
    sb.append(getName());
    sb.append(type.getPostFix());
    return sb.toString();
  }
  
  protected abstract String objToString();
  
  protected abstract Object getObject();
  
  protected abstract boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, IRNode other);
  
  protected abstract boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, ExpressionNode other);
  
  protected abstract boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, HeldInstanceLock other);

  protected abstract boolean testFieldSpecialCase(NeededLock lock, ThisExpressionBinder teb, IBinder b);
  
  /**
   * If the object expression is of the form "this.f", then return the 
   * VariableDeclarator for f.  Otherwise, return <code>null</code>.
   */
  protected abstract IRNode getFieldOfThis(IBinder binder);
  
  protected abstract boolean isFieldExprOfThis(IBinder binder, IRNode varDecl);
}
