
package com.surelogic.aast.java;

public abstract class TypeNode extends ReturnTypeNode { 
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public TypeNode(int offset) {
    super(offset);
  }
}

