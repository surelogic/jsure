package com.surelogic.analysis.nullable;

import java.util.List;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.uwm.cs.fluid.util.AssociativeArrayLattice;

/**
 * Associative array from non-null field declarations to assigned state.
 * This array always contains an extra element whose value is initialized
 * to ASSIGNED.  If the value of this last element is ever not ASSIGNED, then
 * the value of the array is non-normative.
 */
public class AssignedVars extends AssociativeArrayLattice<IRNode, Assigned.Lattice, Assigned> {
  private final Assigned[] empty;
  
  private AssignedVars(final IRNode[] modifiedKeys) {
    super(Assigned.lattice, new Assigned[0], modifiedKeys);

    // Create a unique reference to the empty value
    final int n = modifiedKeys.length;
    empty = new Assigned[n];
    for (int i = 0; i < n-1; i++) empty[i] = Assigned.UNASSIGNED;
    empty[n-1] = Assigned.ASSIGNED;
  }

  public static AssignedVars create(final List<IRNode> keys) {
    final int originalSize = keys.size();
    final IRNode[] modifiedKeys = keys.toArray(new IRNode[originalSize + 1]);
    modifiedKeys[originalSize] = null;
    return new AssignedVars(modifiedKeys);
  }
  
  @Override
  protected boolean indexEquals(final IRNode field1, final IRNode field2) {
    return field1.equals(field2);
  }

  @Override
  public Assigned[] getEmptyValue() {
    return empty;
  }

  @Override
  public boolean isNormal(final Assigned[] value) {
    return value[size-1] == Assigned.ASSIGNED;
  }

  @Override protected String toStringPrefixSeparator() { return "\n"; }
  @Override protected String toStringOpen() { return ""; }
  @Override protected String toStringSeparator() { return "\n"; }
  @Override protected String toStringConnector() { return " "; }
  @Override protected String toStringClose() { return "\n"; }

  @Override
  protected void indexToString(final StringBuilder sb, IRNode field) {
    if (field == null) sb.append("BOGUS");
    else sb.append(VariableDeclarator.getId(field));
  }
}
