
package com.surelogic.aast.promise;


import com.surelogic.aast.java.*;

public abstract class ClassLockExpressionNode extends ExpressionNode { 
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ClassLockExpressionNode(int offset) {
    super(offset);
  }
}

