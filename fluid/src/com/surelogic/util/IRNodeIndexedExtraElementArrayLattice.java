package com.surelogic.util;

import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.uwm.cs.fluid.util.AssociativeArrayLattice;
import edu.uwm.cs.fluid.util.Lattice;

/**
 * Abstract associative array lattice whose keys are IRNodes from the 
 * syntax tree of the program being analyzed, and value contains an extra
 * element at the end that is used to differentiate normal values from 
 * values representing failed analysis.
 */
public abstract class IRNodeIndexedExtraElementArrayLattice<L extends Lattice<T>, T> extends
    AssociativeArrayLattice<IRNode, L, T> {

  protected IRNodeIndexedExtraElementArrayLattice(
//      final L base, final T[] p, final IRNode[] keys) {
      final L base, final IRNode[] keys) {
    super(base, keys);
  }
  
  /**
   * Given a list of the keys for the association, return an array of the keys
   * including the extra element necessary for differentiating normal values 
   * from bad values. 
   */
  protected static IRNode[] modifyKeys(final List<IRNode> keys) {
    final int originalSize = keys.size();
    final IRNode[] modifiedKeys = keys.toArray(new IRNode[originalSize + 1]);
    modifiedKeys[originalSize] = null;
    return modifiedKeys;
  }
  
  /**
   * Get the size of the array ignoring the bogus elements used for
   * testing for normality.
   */
  public final int getRealSize() {
    return size - 1;
  }
  
  @Override
  protected final boolean indexEquals(final IRNode k1, final IRNode k2) {
    return k1.equals(k2);
  }
  
  @Override
  public final boolean isNormal(final T[] value) {
    return value[size-1] == getNormalFlagValue();
  }
  
  protected final T[] createEmptyValue() {
    final T[] empty = newArray();
    for (int i = 0; i < size - 1; i++) empty[i] = getEmptyElementValue();
    empty[size - 1] = getNormalFlagValue();
    return empty;
  }
  
  /**
   * Get the value to use for elements of the empty value.
   */
  protected abstract T getEmptyElementValue();
  
  /**
   * Get the value used in the extra array value that indicates the overall 
   * array value is still normal.
   */
  protected abstract T getNormalFlagValue();
}
