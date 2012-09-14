/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/locks/AASTHeldInstanceLock.java,v 1.6 2009/02/17 14:01:32 aarong Exp $*/
package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.promises.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

class AASTHeldInstanceLock extends HeldInstanceLock {
  private final ExpressionNode objAAST;
  
  AASTHeldInstanceLock(
      final ExpressionNode o2, final LockModel lm, final IRNode src,
      final PromiseDrop<?> sd, final boolean assumed, final Type type) {
    super(lm, src, sd, assumed, type);
    objAAST = o2;
  }

  public HeldLock changeSource(final IRNode newSrc) {
    return new AASTHeldInstanceLock(objAAST, lockPromise, newSrc, supportingDrop, isAssumed, type);
  }

  @Override
  protected String objToString() {
    return objAAST.toString();
  }

  @Override
  protected Object getObject() {
    return objAAST;
  }

  @Override
  protected boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, IRNode other) {
    return checkSyntacticEquality(other, this.objAAST, teb, b);
  }

  @Override
  protected boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, ExpressionNode other) {
    return checkSyntacticEquality(this.objAAST, other, teb, b);
  }

  @Override
  protected boolean checkSyntacticEquality(ThisExpressionBinder teb, IBinder b, HeldInstanceLock other) {
    return other.checkSyntacticEquality(teb, b, this.objAAST);
  }
}
