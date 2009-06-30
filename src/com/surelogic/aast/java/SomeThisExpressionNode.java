
package com.surelogic.aast.java;

import com.surelogic.aast.bind.*;

public abstract class SomeThisExpressionNode extends PrimaryExpressionNode 
implements IHasVariableBinding {
  // Fields

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public SomeThisExpressionNode(int offset) {
    super(offset);
  }
}

