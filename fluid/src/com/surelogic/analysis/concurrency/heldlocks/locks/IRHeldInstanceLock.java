/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/locks/IRHeldInstanceLock.java,v 1.7 2009/02/17 14:01:32 aarong Exp $*/
package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;

class IRHeldInstanceLock extends HeldInstanceLock {
  /**
   * The object-valued expression that this lock is associated with.
   * This is a VariableDeclarator in the case that the lock is "field:lock",
   * in which case it symbolizes a reference to that field via the receiver.
   */
  private final IRNode obj;
  
  IRHeldInstanceLock(
      final IRNode o, final LockModel lm, final IRNode src,
      final PromiseDrop<?> sd, final boolean assumed, final Type type) {
    super(lm, src, sd, assumed, type);
    if (o == null) {
      throw new NullPointerException("obj is null");
    }
    obj = o;
  }

  @Override
  public HeldLock changeSource(final IRNode newSrc) {
    return new IRHeldInstanceLock(obj, lockPromise, newSrc, supportingDrop, isAssumed, type);
  }

  @Override
  protected String objToString() {
    return DebugUnparser.toString(obj);
  }

  @Override
  protected Object getObject() {
    return obj;
  }

  @Override
  protected boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, IRNode other) {
    return checkSyntacticEquality(this.obj, other, teb, b);
  }

  @Override
  protected boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, ExpressionNode other) {
    return checkSyntacticEquality(this.obj, other, teb, b);
  }

  @Override
  protected boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, HeldInstanceLock other) {
    return other.checkSyntacticEquality(teb, b, this.obj);
  }

  @Override
  protected boolean testFieldSpecialCase(NeededLock lock, ThisExpressionBinder teb, IBinder b) {
    if (VariableDeclarator.prototype.includes(obj)) {
      // We have the special case, check to see if the needed lock is "this.f"
      return lock.isFieldExprOfThis(b, obj);
    }
    return false;
  }
  
  @Override
  protected boolean isFieldExprOfThis(IBinder b, IRNode varDecl) {
    // TODO: Probably need to also check to see if "obj" is a VariableDeclarator
    if (FieldRef.prototype.includes(obj)) {
      if (ThisExpression.prototype.includes(FieldRef.getObject(obj))) {
        return b.getBinding(obj).equals(varDecl);
      }
    }
    return false;
  }
}
