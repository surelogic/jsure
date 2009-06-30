
package com.surelogic.aast.java;


import com.surelogic.aast.bind.*;

public abstract class ExpressionNode extends InitializerNode { 
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ExpressionNode(int offset) {
    super(offset);
  }

  public boolean typeExists() {
    return AASTBinder.getInstance().isResolvableToType(this);
  }

  /**
   * Gets the binding corresponding to the type of the Expression
   */
  public IType resolveType() {
    return AASTBinder.getInstance().resolveType(this);
  }

}

