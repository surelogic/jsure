package com.surelogic.analysis.uniqueness.plusFrom.traditional.store;

import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

/**
 * Replace every node that has the variable in it with the new object indicated.
 * @author boyland
 */
public class ReplaceEntire implements Apply {

	private final Object var;
	private final ImmutableHashOrderSet<Object> newObject;
	
	public ReplaceEntire(Object v, ImmutableHashOrderSet<Object> obj) {
		var = v;
		newObject = obj;
	}
	
	@Override
  public ImmutableHashOrderSet<Object> apply(
			ImmutableHashOrderSet<Object> other) {
		if (other.contains(var)) return newObject;
		return other;
	}

}
