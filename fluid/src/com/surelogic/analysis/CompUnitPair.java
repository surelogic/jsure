package com.surelogic.analysis;

import com.surelogic.javac.Projects;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;

public class CompUnitPair implements IAnalysisGranule {
	final IRNode compUnit, node;
	
	public CompUnitPair(IRNode cu, IRNode n) {
		compUnit = cu;
		node = n;
	}
	
	@Override
	public IRNode getCompUnit() {
		return compUnit;
	}
	
	public IRNode getNode() {
		return node;
	}

	@Override
	public ITypeEnvironment getTypeEnv() {
		return Projects.getProject(compUnit).getTypeEnv();
	}

	@Override
	public String getLabel() {
		return JavaNames.getFullName(node);
	}
}
