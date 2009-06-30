
package com.surelogic.aast.java;

public abstract class ConstructionObjectNode extends PrimaryExpressionNode { 
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ConstructionObjectNode(int offset) {
    super(offset);
  }
}

