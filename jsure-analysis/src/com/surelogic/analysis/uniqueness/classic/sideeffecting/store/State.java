package com.surelogic.analysis.uniqueness.classic.sideeffecting.store;

import edu.uwm.cs.fluid.util.AbstractLattice;

/**
 * Replaces the STATE_* constants as well as the PseudoVariable class.  We can
 * use the enumeration elements SHARED, BORROWED, and UNDEFINED directly instead
 * of creating PseudoVariable wrappers.
 */
/**
 * Indication of states of variables in a store lattice.
 * Replaces the STATE_* constants as well as the PseudoVariable class.  We can
 * use the enumeration elements SHARED, BORROWED, and UNDEFINED directly instead
 * of creating PseudoVariable wrappers.
 */
public enum State {
  NULL("null") {
    @Override
    public String getProposedPromiseName() { return null; }
  },
  UNIQUE("@Unique") {
    @Override
    public String getProposedPromiseName() { return "Unique"; }
  },
  SHARED("nothing") {
    @Override
    public String getProposedPromiseName() { return null; }
  },
  BORROWED("@Borrowed") {
    @Override
    public String getProposedPromiseName() { return "Borrowed"; }
  },
  UNDEFINED("undefined") {
    @Override
    public String getProposedPromiseName() { return null; }
  };
  
  private final String annotation;
  
  private State(final String anno) {
    annotation = anno;
  }
  
  public String getAnnotation() {
    return annotation;
  }
  
  public abstract String getProposedPromiseName();
  
  public static class Lattice extends AbstractLattice<State> {
    @Override
    public boolean lessEq(State v1, State v2) {
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
      if (lessEq(v1, v2))
        return v2;
      if (lessEq(v2, v1))
        return v1;
      return BORROWED;
    }

    @Override
    public State meet(State v1, State v2) {
      if (lessEq(v1, v2))
        return v1;
      if (lessEq(v2, v1))
        return v2;
      return UNIQUE;
    }
  }
  
  public static final Lattice lattice = new Lattice();
}
