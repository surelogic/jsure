package com.surelogic.analysis.uniqueness.store;

import edu.uwm.cs.fluid.util.AbstractLattice;

/**
 * Indication of states of variables in a store lattice.
 * Replaces the STATE_* constants as well as the PseudoVariable class.  We can
 * use the enumeration elements SHARED, BORROWED, and UNDEFINED directly instead
 * of creating PseudoVariable wrappers.
 */
public enum State {
  NULL,
  UNIQUE,
  UNIQUEWRITE,
  SHARED,
  IMMUTABLE,
  READONLY,
  BORROWED,
  UNDEFINED;
  
  public static class Lattice extends AbstractLattice<State> {
	@Override
	public boolean lessEq(State v1, State v2) {
		if (v1 == SHARED && v2 == IMMUTABLE) return false;
		return v1.ordinal() <= v2.ordinal();
	}

	@Override
	public State top() {
		return UNDEFINED;
	}

	@Override
	public State bottom() {
		return NULL;
	}

	@Override
	public State join(State v1, State v2) {
		if (lessEq(v1,v2)) return v2;
		if (lessEq(v2,v1)) return v1;
		return READONLY; // the only possibility where neither is under the other
	}

	@Override
	public State meet(State v1, State v2) {
		if (lessEq(v1,v2)) return v1;
		if (lessEq(v2,v1)) return v2;
		return UNIQUEWRITE;
	}
  }
  
  public static final Lattice lattice = new Lattice();
}
