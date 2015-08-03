package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * Represents a field being used a lock.
 */
public final class FieldImplementation extends ClassMemberImplementation {
  // memberDecl is a VariableDeclarator
  public FieldImplementation(final IRNode varDecl) {
    super(varDecl);
  }
  
  /**
   * Get the variable declarator of the field being used as a lock.
   */
  public IRNode getField() {
    return memberDecl;
  }
  
  
  
  @Override
  public String toString() {
    return JavaNames.getFullTypeName(VisitUtil.getEnclosingType(memberDecl)) +
        "." + VariableDeclarator.getId(memberDecl);
  }
  
  @Override
  public int hashCode() {
    return partialHashCode();
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof FieldImplementation) {
      return partialEquals((FieldImplementation) other);
    } else {
      return false;
    }
  }

  
  
  @Override
  protected IJavaType getMemberLockType(final IBinder binder) {
    // memberDecl is a VariableDeclarator
    return binder.getJavaType(memberDecl);
  }
}

