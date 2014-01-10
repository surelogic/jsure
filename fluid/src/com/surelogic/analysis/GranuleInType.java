package com.surelogic.analysis;

import com.surelogic.javac.Projects;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public abstract class GranuleInType implements IAnalysisGranule {
	public final IRNode typeDecl;
	
	protected GranuleInType(IRNode type) {
		typeDecl = type;
	}
	
	public IRNode getCompUnit() {
		return VisitUtil.getEnclosingCompilationUnit(typeDecl);
	}

	public ITypeEnvironment getTypeEnv() {
		return Projects.getEnclosingProject(typeDecl).getTypeEnv();
	}
}
