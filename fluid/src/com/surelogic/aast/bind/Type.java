package com.surelogic.aast.bind;

import com.surelogic.aast.bind.IType;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;

public class Type implements IType {
  protected final IRNode type;
  
  protected Type(IRNode t) {
    type = t;
  }
  
  @Override
  public IJavaType getJavaType() {
    return JavaTypeFactory.getMyThisType(type);
  }

  @Override
  public IRNode getNode() {
    return type;
  }
}
