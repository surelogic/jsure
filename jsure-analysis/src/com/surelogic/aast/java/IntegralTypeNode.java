
package com.surelogic.aast.java;

public abstract class IntegralTypeNode extends NumericTypeNode { 
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public IntegralTypeNode(int offset) {
    super(offset);
  }
}

