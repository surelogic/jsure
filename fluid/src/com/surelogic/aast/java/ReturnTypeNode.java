package com.surelogic.aast.java;


import java.util.logging.Logger;

import com.surelogic.aast.bind.*;
import com.surelogic.aast.*;
import com.surelogic.ast.ResolvableToType;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class ReturnTypeNode extends AASTNode 
implements ResolvableToType { 
  protected static final Logger LOG = SLLogger.getLogger("com.surelogic.aast.java");
  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ReturnTypeNode(int offset) {
    super(offset);
  }

  @Override
  public boolean typeExists() {
    return AASTBinder.getInstance().isResolvableToType(this);
  }

  /**
   * Gets the binding corresponding to the type of the ReturnType
   */
  public IType resolveType() {
    return AASTBinder.getInstance().resolveType(this);
  }
  
  public abstract boolean matches(IRNode type);
}

