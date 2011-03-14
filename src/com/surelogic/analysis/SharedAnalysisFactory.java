package com.surelogic.analysis;

import edu.cmu.cs.fluid.java.bind.IBinder;

public abstract class SharedAnalysisFactory<S> {
	private final Class<S> group;

	protected SharedAnalysisFactory(Class<S> g) {
		group = g;
	}
	
	Class<S> getGroup() {
		return group;
	}

	protected abstract S create(IBinder b);
	protected abstract void clear();
}
