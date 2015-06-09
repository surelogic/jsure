package com.surelogic.analysis.uniqueness.plusFrom.traditional.store;

import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

/**
 * Find every object referenced by the variable.
 * If there is already a pseudo-variable there, remove it.
 * Add the new pseudo-variable.
 * @author boyland
 */
public class Downgrade implements Apply {

	private final Object var;
	private final State newState;
	
	public Downgrade(Object v, State ns) {
		var = v;
		newState = ns;
	}
	
	private static ImmutableHashOrderSet<Object> pseudos = new ImmutableHashOrderSet<Object>(State.values());
	
	@Override
  public ImmutableHashOrderSet<Object> apply(
			ImmutableHashOrderSet<Object> other) {
		// we don't change immutable objects (cannot be downgraded)
//		if (other.contains(State.IMMUTABLE)) return other;
		if (other.contains(var)) {
			for (Object v : other) {
				if (v == newState) return other;
				if (v instanceof State) {
					// cannot downgrade
					if (!State.lattice.lessEq((State)v,newState)) return other;
				}
			}
			return other.difference(pseudos).addElement(newState);
		}
		return other;
	}

}
