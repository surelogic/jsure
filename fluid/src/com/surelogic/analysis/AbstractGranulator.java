package com.surelogic.analysis;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;

public abstract class AbstractGranulator<T extends IAnalysisGranule> implements IAnalysisGranulator<T> {
	private final Class<T> type;
	private final List<T> granules = new ArrayList<T>();
	
	protected AbstractGranulator(Class<T> t) {		
		type = t;
	}
	
	public Class<T> getType() {
		return type;
	}
	
	public final List<T> getGranules() {
		return granules; // TODO clear?
	}

	// TODO not thread-safe
	public final int extractGranules(IRNode cu) {
		final int orig = granules.size();
		extractGranules(granules, cu);
		return granules.size() - orig;
	}
	
	protected abstract void extractGranules(List<T> granules, IRNode cu);
}
