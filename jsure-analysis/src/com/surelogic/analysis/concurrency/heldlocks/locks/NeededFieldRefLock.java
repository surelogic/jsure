package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.MethodCallUtils.EnclosingRefs;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser; 
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;

final class NeededFieldRefLock extends AbstractNeededInstanceLock {
  /**
   * VariableDeclarator of the field that is being referenced.
   */
  private final IRNode varDecl;
  
  

  NeededFieldRefLock(final IRNode o, final IRNode vd, final LockModel lm, final Type type) {
    super(o, lm, type);
    varDecl = vd;
  }
  
  @Override
  public boolean equals(final Object o) {
    if (o instanceof NeededFieldRefLock) {
      final NeededFieldRefLock other = (NeededFieldRefLock) o;
      return baseEquals(other) && obj.equals(other.obj) && varDecl.equals(other.varDecl);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append('<');
    sb.append(DebugUnparser.toString(obj));
    sb.append('.');
    sb.append(VariableDeclarator.getId(varDecl));
    sb.append(">:");
    sb.append(getName());
    sb.append(type.getPostFix());
    return sb.toString();
  }

  @Override
  boolean satisfiesAAST(
      final AASTHeldInstanceLock lock, final ThisExpressionBinder teb) {
    return checkFieldRef(teb, obj, varDecl, lock.objAAST);
  }

  @Override
  boolean satisfiesIR(
      final IRHeldInstanceLock lock, final ThisExpressionBinder teb) {
    return checkFieldRef(teb, obj, varDecl, lock.obj);
  }

  @Override
  boolean satisfiesFieldRef(
      final HeldFieldRefLock lock, final ThisExpressionBinder teb) {
    return varDecl.equals(lock.varDecl)
        && checkSyntacticEquality(obj, lock.obj, teb);
  }

  @Override
  public boolean mayHaveAliasInCallingContext() {
    return false;
  }
  
  @Override
  public NeededLock getAliasInCallingContext(
      final EnclosingRefs enclosingRefs, final NeededLockFactory lockFactory) {
    return null;
  }
}
