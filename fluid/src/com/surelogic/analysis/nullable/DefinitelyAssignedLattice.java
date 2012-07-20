package com.surelogic.analysis.nullable;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.uwm.cs.fluid.util.TripleLattice;

final class DefinitelyAssignedLattice
    extends TripleLattice<Assigned[], Assigned[], Assigned[], DATriple,
        AssignedVars, AssignedVars, AssignedVars> {
  public DefinitelyAssignedLattice(final AssignedVars avLattice) {
    super(avLattice, avLattice, avLattice);
  }

  @Override
  protected DATriple newTriple(
      final Assigned[] n, final Assigned[] t, final Assigned[] f) {
    return new DATriple(n, t, f);
  }

  

  public boolean isNormal(final DATriple v) {
    return lattice1.isNormal(v.normal());
  }
  
  public DATriple dropWhens(final DATriple v) {
    return canonicalize(
        newTriple(v.normal(), lattice2.top(), lattice3.top()), v);
  }
  
  public DATriple addWhens(final DATriple v) {
    return canonicalize(newTriple(v.normal(), v.normal(), v.normal()), v);
  }
  
  public DATriple set(final DATriple v, final IRNode field, final Assigned n) {
    final int idx = lattice1.indexOf(field);
    if (idx != -1) {
      return replaceFirst(v, lattice1.replaceValue(v.normal(), idx, n));
    } else {
      return v;
    }
  }
  
  public DATriple set(final DATriple v, final IRNode field,
      final Assigned n, final Assigned t, final Assigned f) {
    final int idx = lattice1.indexOf(field);
    if (idx != -1) {
      return canonicalize(
          newTriple(
              lattice1.replaceValue(v.normal(), idx, n),
              lattice2.replaceValue(v.whenTrue(), idx, t),
              lattice3.replaceValue(v.whenFalse(), idx, f)),
          v);
    } else {
      return v;
    }
  }
}
