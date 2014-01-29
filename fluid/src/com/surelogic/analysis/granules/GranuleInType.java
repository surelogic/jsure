package com.surelogic.analysis.granules;

import com.surelogic.javac.Projects;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public abstract class GranuleInType implements IAnalysisGranule {
	public final IRNode typeDecl;
	
	protected GranuleInType(IRNode type) {
		typeDecl = type;
	}
	
	@Override
  public IRNode getCompUnit() {
		return VisitUtil.getEnclosingCompilationUnit(typeDecl);
	}

	@Override
  public ITypeEnvironment getTypeEnv() {
		return Projects.getEnclosingProject(typeDecl).getTypeEnv();
	}
	
	@Override
  public boolean isAsSource() {
		return true;
	}
}
