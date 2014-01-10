package com.surelogic.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public class TypeAndMethod extends GranuleInType {
	public final IRNode methodDecl;
	
	public TypeAndMethod(IRNode type, IRNode method) {
		super(type);
		methodDecl = method;
	}

	public String getLabel() {
		return JavaNames.getFullName(methodDecl);
	}
	
    public IRNode getClassBody() {
        return VisitUtil.getClassBody(typeDecl);
    }

    @Override
    public boolean equals(final Object other) {
    	if (other instanceof TypeAndMethod) {
    		final TypeAndMethod tan = (TypeAndMethod) other;
    		return typeDecl == tan.typeDecl && methodDecl == tan.methodDecl;
    	} else {
    		return false;
    	}
    }

    @Override
    public int hashCode() {
    	int result = 17;
    	result = 31 * result + typeDecl.hashCode();
    	result = 31 * result + methodDecl.hashCode();
    	return result;
    }
}
