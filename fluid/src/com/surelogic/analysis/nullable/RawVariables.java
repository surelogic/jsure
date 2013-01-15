package com.surelogic.analysis.nullable;

import com.sun.tools.javac.util.List;
import com.surelogic.analysis.nullable.RawLattice.Element;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.uwm.cs.fluid.util.AssociativeArrayLattice;

/**
 * Associative array from the receiver declaration (if any), and any non-array
 * object-typed local variable and parameter declarations to the raw state of
 * the referenced object. This array always contains an extra element indexed by
 * "null" whose value is initialized to IMPOSSIBLE. If the value of this last
 * element is ever not IMPOSSIBLE, then the value of the array is non-normative.
 */
public final class RawVariables extends AssociativeArrayLattice<IRNode, RawLattice, RawLattice.Element> {
  private final Element[] empty;

  private RawVariables(
      final IRNode[] modifiedKeys, final RawLattice lat) {
    super(lat, RawLattice.ARRAY_PROTOTYPE, modifiedKeys);

    // Create a unique reference to the empty value
    final int n = modifiedKeys.length;
    empty = new Element[n];
    for (int i = 0; i < n-1; i++) empty[i] = RawLattice.NOT_RAW;
    empty[n-1] = RawLattice.IMPOSSIBLE;
  }

  public static RawVariables create(
      final List<IRNode> keys, final RawLattice lattice) {
    final int originalSize = keys.size();
    final IRNode[] modifiedKeys = keys.toArray(new IRNode[originalSize + 1]);
    modifiedKeys[originalSize] = null;
    return new RawVariables(modifiedKeys, lattice);
  }
  
  @Override
  protected boolean indexEquals(final IRNode field1, final IRNode field2) {
    return field1.equals(field2);
  }

  @Override
  public Element[] getEmptyValue() {
    return empty;
  }

  @Override
  public boolean isNormal(final Element[] value) {
    return value[size-1] == RawLattice.IMPOSSIBLE;
  }
  
  @Override
  protected void indexToString(final StringBuilder sb, final IRNode index) {
    if (index == null) {
      sb.append("BOGUS");
    } else {
      final Operator op = JJNode.tree.getOperator(index);
      if (ReceiverDeclaration.prototype.includes(op)) {
        sb.append("this");
      } else if (ParameterDeclaration.prototype.includes(op)) {
        sb.append(ParameterDeclaration.getId(index));
      } else { // VariableDeclarator
        sb.append(VariableDeclarator.getId(index));
      }
    }
  }
}
