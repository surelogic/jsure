package com.surelogic.analysis.granules;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import extra166y.Ops.Procedure;

public abstract class AbstractGranulator<T extends IAnalysisGranule> implements IAnalysisGranulator<T> {
	private final Class<T> type;
	private final List<T> granules = new ArrayList<T>();
	
	protected AbstractGranulator(Class<T> t) {		
		type = t;
	}
	
	@Override
  public Class<T> getType() {
		return type;
	}
	
	@Override
  public final List<T> getGranules() {
		try {
			return new ArrayList<T>(granules); 
		} finally {
			granules.clear();
		}
	}

	// TODO not thread-safe
	@Override
  public final int extractGranules(ITypeEnvironment tEnv, IRNode cu) {
		final int orig = granules.size();
		extractGranules(granules, tEnv, cu);
		return granules.size() - orig;
	}
	
	protected abstract void extractGranules(List<T> granules, ITypeEnvironment tEnv, IRNode cu);
	
	public Procedure<T> wrapAnalysis(Procedure<T> proc) {
		return proc;
	}
}
