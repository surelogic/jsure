
package com.surelogic.aast.java;


import com.surelogic.aast.bind.*;

public abstract class ClassTypeNode extends ReferenceTypeNode { 
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ClassTypeNode(int offset) {
    super(offset);
  }

  @Override
  public boolean typeExists() {
    return AASTBinder.getInstance().isResolvableToType(this);
  }

  /**
   * Gets the binding corresponding to the type of the ClassType
   */
  @Override
  public ISourceRefType resolveType() {
    return AASTBinder.getInstance().resolveType(this);
  }
}

