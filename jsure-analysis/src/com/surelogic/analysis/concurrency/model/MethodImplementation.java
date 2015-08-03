package com.surelogic.analysis.concurrency.model;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;

/**
 * Represents a field being used a lock.
 */
public final class MethodImplementation extends ClassMemberImplementation {
  public MethodImplementation(final IRNode methodDecl) {
    super(methodDecl);
  }
  
  /**
   * Get the method declaration of the method that returns the lock.
   */
  public IRNode getMethod() {
    return memberDecl;
  }
  
  
  
  @Override
  public String toString() {
    return JavaNames.genMethodConstructorName(memberDecl) + "()";
  }
  
  @Override
  public int hashCode() {
    return partialHashCode();
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof MethodImplementation) {
      return partialEquals((MethodImplementation) other);
    } else {
      return false;
    }
  }

  
  
  @Override
  protected IJavaType getMemberLockType(final IBinder binder) {
    // memberDecl is a MethodDeclaration
    return JavaTypeFactory.convertNodeTypeToIJavaType(
        MethodDeclaration.getReturnType(memberDecl), binder);
  }
}
