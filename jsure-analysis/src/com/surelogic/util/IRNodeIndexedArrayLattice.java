package com.surelogic.util;

import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.uwm.cs.fluid.util.AssociativeArrayLattice;
import edu.uwm.cs.fluid.util.Lattice;

/**
 * Abstract associative array lattice whose keys are IRNodes from the 
 * syntax tree of the program being analyzed.
 */
public abstract class IRNodeIndexedArrayLattice<L extends Lattice<T>, T> extends
    AssociativeArrayLattice<IRNode, L, T> {
  protected IRNodeIndexedArrayLattice(
      final L base, final IRNode[] keys) {
    super(base, keys);
  }

  protected IRNodeIndexedArrayLattice(
      final L base, final List<IRNode> keyList) {
    this(base, keyList.toArray(new IRNode[keyList.size()]));
  }

  
  
  @Override
  protected final boolean indexEquals(final IRNode k1, final IRNode k2) {
    return k1.equals(k2);
  }
  
  protected T[] createEmptyValue() {
    final T[] empty = newArray();
    for (int i = 0; i < size; i++) empty[i] = getEmptyElementValue();
    return empty;
  }
  
  /**
   * Get the value to use for elements of the empty value.
   */
  protected abstract T getEmptyElementValue();
}
