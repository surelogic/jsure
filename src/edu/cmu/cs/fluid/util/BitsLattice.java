package edu.cmu.cs.fluid.util;

/** Lattice values represented by bits.  We represent
 * all such lattices with a single class which caches the
 * values for optimal use.  The lattice implements a lattice with
 * <pre>meet(v1,v2) = v1 | v2</pre>.
 * @see FlatLattice
 * 
 * @region private static Table
 * @lock TableLock is class protects Table
 */
public class BitsLattice implements Lattice {
  private final int bits;
  
  private BitsLattice(int i) {
    bits = i;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof BitsLattice) {
      BitsLattice bl = (BitsLattice)other;
      return bits == bl.bits;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return bits;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    int i = bits;
    boolean negative = false;
    if (i < 0) {
      i = -i;
      negative = true;
    }
    if (i == 0) {
      b.append("0");
    } else {
      while (i != 0) {
	b.append((i & 1) == 1 ? "1" : "0");
	i >>>= 1;
      }
    }
    if (negative) b.append("~");
    return b.reverse().toString();
  }

  public Lattice top() {
    return newInteger(0);
  }

  public Lattice bottom() {
    return newInteger(-1);
  }

  public Lattice meet(Lattice other) {
    BitsLattice bl = (BitsLattice)other;
    return newInteger(bits|bl.bits);
  }

  public Lattice join(Lattice other) {
    BitsLattice bl = (BitsLattice)other;
    return newInteger(bits&bl.bits);
  }

  public boolean includes(Lattice other) {
    BitsLattice bl = (BitsLattice)other;
    return (bits|bl.bits) == bl.bits;
  }
  
  /**
   * @unique
   * @aggregate Instance into Table  
   */
  private static final IntegerTable bitslattices = new IntegerTable() {
    @Override
    protected Object box(int i) { return new BitsLattice(i); }
  };
  
  /** Return an integer as a bits lattice value */
  public static synchronized BitsLattice newInteger(int i) {
    return (BitsLattice)bitslattices.get(i);
  }
}

