package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store;

import edu.uwm.cs.fluid.util.AbstractLattice;

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
  UNIQUEWRITE("@Unique(allowRead=true)") {
    @Override
    public String getProposedPromiseName() { return "Unique"; } // TODO add allowRead property
  },
  SHARED("nothing") {
    @Override
    public String getProposedPromiseName() { return null; }
  },
  IMMUTABLE("@Immutable") {
    @Override
    public String getProposedPromiseName() { return "Immutable"; }
  },
  READONLY("@ReadOnly") {
    @Override
    public String getProposedPromiseName() { return "ReadOnly"; }
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
    public boolean lessEq(State v1, State v2) {
      if (v1 == SHARED && v2 == IMMUTABLE)
        return false;
      return v1.ordinal() <= v2.ordinal();
    }

    public State top() {
      return UNDEFINED;
    }

    public State bottom() {
      return NULL;
    }

    public State join(State v1, State v2) {
      if (lessEq(v1, v2))
        return v2;
      if (lessEq(v2, v1))
        return v1;
      return READONLY; // the only possibility where neither is under the other
    }

    public State meet(State v1, State v2) {
      if (lessEq(v1, v2))
        return v1;
      if (lessEq(v2, v1))
        return v2;
      return UNIQUEWRITE;
    }
  }
  
  public static final Lattice lattice = new Lattice();
}
