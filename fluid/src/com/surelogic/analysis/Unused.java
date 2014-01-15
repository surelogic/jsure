package com.surelogic.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;

/**
 * A placeholder for use with ConcurrentAnalysis type arguments
 * @author Edwin
 */
public final class Unused implements IAnalysisGranule {
	private Unused() {}

	@Override
	public IRNode getCompUnit() {
		return null;
	}

	@Override
	public ITypeEnvironment getTypeEnv() {
		return null;
	}

	@Override
	public String getLabel() {
		return null;
	}

	@Override
	public boolean isAsSource() {
		// TODO Auto-generated method stub
		return false;
	}
}
