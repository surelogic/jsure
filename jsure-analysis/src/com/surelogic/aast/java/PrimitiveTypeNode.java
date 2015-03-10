
package com.surelogic.aast.java;


import com.surelogic.aast.bind.*;

public abstract class PrimitiveTypeNode extends TypeNode 
{ 
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public PrimitiveTypeNode(int offset) {
    super(offset);
  }

  @Override
  public boolean typeExists() {
    return AASTBinder.getInstance().isResolvableToType(this);
  }

  /**
   * Gets the binding corresponding to the type of the PrimitiveType
   */
  @Override
  public IPrimitiveType resolveType() {
    return AASTBinder.getInstance().resolveType(this);
  }

}

