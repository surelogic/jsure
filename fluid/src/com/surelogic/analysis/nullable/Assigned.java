package com.surelogic.analysis.nullable;

import edu.uwm.cs.fluid.util.AbstractLattice;

public enum Assigned {
  BOTTOM,
  ASSIGNED,
  UNASSIGNED,
  TOP;
  
  public static final Assigned[] NO_ASSIGNED = new Assigned[0];
  
  public static class Lattice extends AbstractLattice<Assigned> {
    private Lattice() {
      super();
    }
    
    @Override
    public boolean lessEq(final Assigned v1, final Assigned v2) {
      // Simple linear relationship between the elements
      return v1.ordinal() <= v2.ordinal();
    }

    @Override
    public Assigned top() {
      return TOP;
    }

    @Override
    public Assigned bottom() {
      return BOTTOM;
    }

    @Override
    public Assigned join(final Assigned v1, final Assigned v2) {
      // return whichever is greatest
      if (lessEq(v1, v2)) return v2;
      if (lessEq(v2, v1)) return v1;
      // shouldn't get here
      throw new UnsupportedOperationException(
          "Can't get join of " + v1 + " and " + v2);
    }

    @Override
    public Assigned meet(final Assigned v1, final Assigned v2) {
      // return whichever is least
      if (lessEq(v1, v2)) return v1;
      if (lessEq(v2, v1)) return v2;
      // shouldn't get here
      throw new UnsupportedOperationException(
          "Can't get meet of " + v1 + " and " + v2);
    }
  }
  
  public static final Lattice lattice = new Lattice();
}
