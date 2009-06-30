package edu.cmu.cs.fluid.util;

/** A lattice formed using an array of lattices, all the same type. */
public class ArrayLattice<T> extends RecordLattice<T> {

  @SuppressWarnings("unchecked")
  private static <T> Lattice<T>[] makeBaseLattices(Lattice<T> baseLattice, int size) {
    Lattice<T>[] baseLattices = new Lattice[size];
    for (int i=0; i < size; ++i) {
      baseLattices[i] = baseLattice;
    }
    return baseLattices;
  }
  
  public ArrayLattice(Lattice<T> baseLattice, int size) {
    super(makeBaseLattices(baseLattice,size));
  }

  protected ArrayLattice(Lattice<T>[] v, RecordLattice<T> t, RecordLattice<T> b) {
    super(v,t,b);
  }

  @Override
  protected RecordLattice<T> newLattice(Lattice<T>[] values) {
    return new ArrayLattice<T>(values,top,bottom);
  }
}
