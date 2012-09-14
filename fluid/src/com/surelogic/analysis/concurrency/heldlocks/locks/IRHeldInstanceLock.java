/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/locks/IRHeldInstanceLock.java,v 1.7 2009/02/17 14:01:32 aarong Exp $*/
package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;

class IRHeldInstanceLock extends HeldInstanceLock {
  /**
   * The object-valued expression that this lock is associated with.
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
}
