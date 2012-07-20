package com.surelogic.analysis.nullable;

import edu.cmu.cs.fluid.util.Triple;

final class DATriple extends Triple<Assigned[], Assigned[], Assigned[]>{
  public DATriple(final Assigned[] n, final Assigned[] t, final Assigned[] f) {
    super(n, t, f);
  }

  public Assigned[] normal() { return elem1; }
  public Assigned[] whenTrue() { return elem2; }
  public Assigned[] whenFalse() { return elem3; }
  
  public Assigned normal(final int i) { return elem1[i]; }
  public Assigned whenTrue(final int i) { return elem2[i]; }
  public Assigned whenFalse(final int i) { return elem3[i]; }
}
