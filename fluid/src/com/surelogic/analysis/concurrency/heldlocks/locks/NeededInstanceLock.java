package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.aast.java.FieldRefNode;
import com.surelogic.aast.java.ThisExpressionNode;
import com.surelogic.analysis.MethodCallUtils.EnclosingRefs;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser; 
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;

final class NeededInstanceLock extends AbstractNeededLock {
  /**
	 * The object-valued expression that this lock is associated with.
	 * This is variable declarator in the case that the lock is "field:lock", 
	 * in which case it symbolizes that the lock expression is really
	 * "this.field:lock".
	 */
  private final IRNode obj;

  NeededInstanceLock(final IRNode o, final LockModel lm, final Type type) {
    super(lm, type);
    obj = o;
  }
  
  /* Default visibility: only needs to be used by the implementation of
   * HeldInstanceLock.
   */ 
  IRNode getObject() {
    return obj;
  }
  
  @Override
  public boolean equals(final Object o) {
    if (o instanceof NeededInstanceLock) {
      final NeededInstanceLock other = (NeededInstanceLock) o;
      return baseEquals(other) && obj.equals(other.obj);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append('<');
    sb.append(DebugUnparser.toString(obj));
    sb.append(">:");
    sb.append(getName());
    sb.append(type.getPostFix());
    return sb.toString();
  }

  @Override
  public boolean mayHaveAliasInCallingContext() {
    return QualifiedReceiverDeclaration.prototype.includes(obj);
  }
  
  @Override
  public NeededLock getAliasInCallingContext(
      final EnclosingRefs enclosingRefs, final NeededLockFactory lockFactory) {
    final IRNode newObj = enclosingRefs.replace(obj);
    if (newObj != null) {
      return lockFactory.createInstanceLock(newObj, lockPromise, type);
    } else {
      return null;
    }
  }
  
  @Override
  public boolean isFieldExprOfThis(final IBinder b, final IRNode varDecl) {
    // See if we are also a special case
    if (obj.equals(varDecl)) {
      return true;
    } else {
      if (FieldRef.prototype.includes(obj)) {
        if (ThisExpression.prototype.includes(FieldRef.getObject(obj))) {
          return b.getBinding(obj).equals(varDecl);
        }
      }
      return false;
    }
  }
}
