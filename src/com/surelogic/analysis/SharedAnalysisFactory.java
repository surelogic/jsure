package com.surelogic.analysis;

import java.util.*;

import edu.cmu.cs.fluid.java.bind.IBinder;

public abstract class SharedAnalysisFactory<S> {
	private final Class<S> group;
	private final Map<IBinder, S> shared = new HashMap<IBinder, S>();
	
	protected SharedAnalysisFactory(Class<S> g) {
		group = g;
	}
	
	Class<S> getGroup() {
		return group;
	}

	final S get(IBinder b) {
		S s = shared.get(b);
		if (s == null) {
			s = create(b);
			shared.put(b, s);
		}
		return s;
	}
	
	final void clear() {
		shared.clear();
	}
	
	/**
	 * Creates the instance to be cached above
	 */
	protected abstract S create(IBinder b);

}
