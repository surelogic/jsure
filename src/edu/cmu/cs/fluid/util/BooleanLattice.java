/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/BooleanLattice.java,v 1.4 2007/03/09 21:54:19 chance Exp $ */
package edu.cmu.cs.fluid.util;

public class BooleanLattice implements Lattice<Boolean> {
  private final boolean value, trueIsTop;

  private BooleanLattice(boolean val, boolean tIsTop) {
    value = val;
    trueIsTop = tIsTop;
  }

  private static final BooleanLattice andLatticeTop =
    new BooleanLattice(true,true);
  private static final BooleanLattice andLatticeBottom =
    new BooleanLattice(false,true);
  private static final BooleanLattice orLatticeTop =
    new BooleanLattice(false,false);
  private static final BooleanLattice orLatticeBottom =
    new BooleanLattice(true,false);

  public static final BooleanLattice andLattice = andLatticeTop;
  public static final BooleanLattice orLattice = orLatticeTop;

  public boolean getBoolean() {
    return value;
  }
  public BooleanLattice make(boolean bool) {
    return (BooleanLattice)(bool == trueIsTop ? top() : bottom());
  }

  public Lattice<Boolean> top() {
    return trueIsTop ? andLatticeTop : orLatticeTop;
  }
  public Lattice<Boolean> bottom() {
    return trueIsTop ? andLatticeBottom : orLatticeBottom;
  }

  public Lattice<Boolean> meet(Lattice<Boolean> otherL) {
    BooleanLattice other = (BooleanLattice)otherL;
    if (other == this) return other;
    else return bottom();
  }

  public Lattice<Boolean> join(Lattice<Boolean> otherL) {
    BooleanLattice other = (BooleanLattice)otherL;
    if (other == this) return other;
    else return top();
  }

  public boolean includes(Lattice<Boolean> otherL) {
    BooleanLattice other = (BooleanLattice)otherL;
    return (this == other) || (this == top());
  }
  
  public final boolean getValue() {
    return value;
  }
}
