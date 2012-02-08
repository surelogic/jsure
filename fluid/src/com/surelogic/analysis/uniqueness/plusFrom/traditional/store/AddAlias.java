package com.surelogic.analysis.uniqueness.plusFrom.traditional.store;

import java.util.Set;

import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

public class AddAlias implements Apply {

	private final Set<ImmutableHashOrderSet<Object>> aliasNodes;
	private final Object newAlias;
	
	public AddAlias(Set<ImmutableHashOrderSet<Object>> aliases, Object var) {
		aliasNodes = aliases;
		newAlias = var;
	}
	
	public ImmutableHashOrderSet<Object> apply(
			ImmutableHashOrderSet<Object> other) {
		if (aliasNodes.contains(other))
			return other.addElement(newAlias);
		else
			return other;
	}

}
