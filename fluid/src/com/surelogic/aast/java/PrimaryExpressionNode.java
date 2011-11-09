
package com.surelogic.aast.java;

public abstract class PrimaryExpressionNode extends ExpressionNode { 
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public PrimaryExpressionNode(int offset) {
    super(offset);
  }
}

