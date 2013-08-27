package com.surelogic.analysis.nullable2;

import java.util.List;

import com.surelogic.util.IRNodeIndexedExtraElementArrayLattice;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;

/**
 * Associative array from non-null field declarations to assigned state.
 * This array always contains an extra element indexed by "null" whose value is initialized
 * to ASSIGNED.  If the value of this last element is ever not ASSIGNED, then
 * the value of the array is non-normative.
 */
public final class AssignedVars extends IRNodeIndexedExtraElementArrayLattice<Assigned.Lattice, Assigned> {
  private final Assigned[] empty;
  private final Assigned[] allAssigned;
  
  private AssignedVars(final IRNode[] modifiedKeys) {
    super(Assigned.lattice, modifiedKeys);

    // Create a unique reference to the empty value
    empty = createEmptyValue();
    
    // Create a unique reference to the all assigned value
    allAssigned = new Assigned[size];
    for (int i = 0; i < size; i++) allAssigned[i] = Assigned.ASSIGNED;
  }

  public static AssignedVars create(final List<IRNode> keys) {
    return new AssignedVars(modifyKeys(keys));
  }

  @Override
  public Assigned[] getEmptyValue() {
    return empty;
  }
  
  public Assigned[] getAllAssigned() {
    return allAssigned;
  }

  @Override
  protected Assigned[] newArray() {
    return new Assigned[size];
  }
  
  @Override
  protected Assigned getEmptyElementValue() { return Assigned.UNASSIGNED; }
  
  @Override
  protected Assigned getNormalFlagValue() { return Assigned.ASSIGNED; }

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
