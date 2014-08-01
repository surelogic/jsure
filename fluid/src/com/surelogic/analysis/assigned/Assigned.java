package com.surelogic.analysis.assigned;

import edu.uwm.cs.fluid.util.AbstractLattice;

public enum Assigned {
  BOTTOM {
    @Override
    public boolean lessEq(final Assigned other) {
      return true;
    }
    
    @Override
    public Assigned join(final Assigned other) {
      return other;
    }
    
    @Override
    public Assigned meet(final Assigned other) {
      return this;
    }
  },

  PROVABLY_ASSIGNED { // Definitely Assigned, NOT Definitely Unassigned
    @Override
    public boolean lessEq(final Assigned other) {
      return other != BOTTOM && other != PROVABLY_UNASSIGNED;
    }
    
    @Override
    public Assigned join(final Assigned other) {
      if (other == BOTTOM) return this;
      else if (other == PROVABLY_UNASSIGNED) return NOT_SURE;
      else return other;
    }
    
    @Override
    public Assigned meet(final Assigned other) {
      if (other == BOTTOM || other == PROVABLY_UNASSIGNED) return BOTTOM;
      else return this;
    }
  },
  
  PROVABLY_UNASSIGNED { // NOT Definitely Assigned, Definitely Unassigned
    @Override
    public boolean lessEq(final Assigned other) {
      return other != BOTTOM && other != PROVABLY_ASSIGNED;
    }
    
    @Override
    public Assigned join(final Assigned other) {
      if (other == BOTTOM) return this;
      else if (other == PROVABLY_ASSIGNED) return NOT_SURE;
      else return other;
    }
    
    @Override
    public Assigned meet(final Assigned other) {
      if (other == BOTTOM || other == PROVABLY_ASSIGNED) return BOTTOM;
      else return this;
    }
  },
  
  NOT_SURE { // NOT Definitely Assigned, NOT Definitely Unassigned
    @Override
    public boolean lessEq(final Assigned other) {
      return other == NOT_SURE || other == IMPOSSIBLE;
    }
    
    @Override
    public Assigned join(final Assigned other) {
      return other == IMPOSSIBLE ? other : this;
    }
    
    @Override
    public Assigned meet(final Assigned other) {
      return other == IMPOSSIBLE ? this : other;
    }
  },
  
  IMPOSSIBLE { // TOP: Definitely Assigned, Definitely Unassigned
    @Override
    public boolean lessEq(final Assigned other) {
      return other == this;
    }
    
    @Override
    public Assigned join(final Assigned other) {
      return this;
    }
    
    @Override
    public Assigned meet(final Assigned other) {
      return other;
    }
  };
  
  public abstract boolean lessEq(Assigned other);
  public abstract Assigned join(Assigned other);
  public abstract Assigned meet(Assigned other);

  
  
  public static final Assigned[] ARRAY_PROTOTYPE = new Assigned[0];
  
  public static class Lattice extends AbstractLattice<Assigned> {
    private Lattice() {
      super();
    }
    
    @Override
    public Assigned top() {
      return IMPOSSIBLE;
    }
    
    @Override
    public Assigned bottom() {
      return BOTTOM;
    }
    
    @Override
    public boolean lessEq(final Assigned v1, final Assigned v2) {
      return v1.lessEq(v2);
    }
    
    @Override
    public Assigned join(final Assigned v1, final Assigned v2) {
      return v1.join(v2);
    }
    
    @Override
    public Assigned meet(final Assigned v1, final Assigned v2) {
      return v1.meet(v2);
    }
  }
  
  public static final Lattice lattice = new Lattice();
}

