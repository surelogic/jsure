package com.surelogic.analysis;

import java.util.*;

import edu.cmu.cs.fluid.java.bind.IBinder;

public abstract class SharedAnalysisFactory<S> {
	private final Class<S> group;
	private final Map<IBinder, S> shared = new HashMap<IBinder, S>();
	private final boolean share;
	
	protected SharedAnalysisFactory(Class<S> g, boolean share) {
		group = g;
		this.share = share;
	}
	
	Class<S> getGroup() {
		return share ? group : null;
	}

	final S get(IBinder b) {
		if (!share) {
			return create(b);
		}
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
