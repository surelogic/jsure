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
  /**
   * Whether final fields are protected.  This is only true when this 
   * implementation is for field 'f' and is associated with the field 'f'
   * via GuardedBy("itself") on field 'f'.  
   */
  private final boolean protectsFinal;
  
  // memberDecl is a VariableDeclarator
  public FieldImplementation(final IRNode varDecl, final boolean protectsFinal) {
    super(varDecl);
    this.protectsFinal = protectsFinal;
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
  public String getPostfixId() {
    return "." + VariableDeclarator.getId(memberDecl);
  }

  @Override
  public boolean isFinalProtected() { return protectsFinal; }
  
  @Override
  protected IJavaType getMemberLockType(final IBinder binder) {
    // memberDecl is a VariableDeclarator
    return binder.getJavaType(memberDecl);
  }
}

