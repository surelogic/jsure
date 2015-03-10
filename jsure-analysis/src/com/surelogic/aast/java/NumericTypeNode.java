
package com.surelogic.aast.java;

public abstract class NumericTypeNode extends PrimitiveTypeNode { 
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public NumericTypeNode(int offset) {
    super(offset);
  }
}

