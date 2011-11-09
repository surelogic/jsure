
package com.surelogic.aast.java;


import com.surelogic.aast.*;

public abstract class InitializerNode extends AASTNode { 
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public InitializerNode(int offset) {
    super(offset);
  }
}

