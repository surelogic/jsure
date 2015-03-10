package com.surelogic.analysis.granules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public final class TypeAndMethod extends GranuleInType {
	protected final IRNode methodDecl;
	
	public TypeAndMethod(final IRNode type, final IRNode method) {
		super(type);
		methodDecl = method;
	}

	public IRNode getMethod() {
	  return methodDecl;
	}
	
	@Override
  public IRNode getNode() {
		return methodDecl;
	}
	
	@Override
  public String getLabel() {
		return JavaNames.getFullName(methodDecl);
	}
	
  public IRNode getClassBody() {
    return VisitUtil.getClassBody(typeDecl);
  }

  @Override
  public boolean equals(final Object other) {
    if (other instanceof TypeAndMethod) {
      final TypeAndMethod o = (TypeAndMethod) other;
      return typeDecl == o.typeDecl && methodDecl == o.methodDecl;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + typeDecl.hashCode();
    if (methodDecl != null) {
      result = 31 * result + methodDecl.hashCode();
    }
    return result;
  }
}
