package com.surelogic.analysis.granules;

import com.surelogic.javac.Projects;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public abstract class GranuleInType implements IAnalysisGranule {
	protected final IRNode typeDecl;
	
	protected GranuleInType(final IRNode type) {
		typeDecl = type;
	}
	
	public final IRNode getType() {
	  return typeDecl;
	}
	
	/**
	 * Purposely leave the node of the granule unspecified.
	 */
	@Override
	public abstract IRNode getNode();
	
	@Override
  public final IRNode getCompUnit() {
		return VisitUtil.getEnclosingCompilationUnit(typeDecl);
	}

	@Override
  public final ITypeEnvironment getTypeEnv() {
		return Projects.getEnclosingProject(typeDecl).getTypeEnv();
	}
	
	@Override
  public final boolean isAsSource() {
		return true;
	}
}
