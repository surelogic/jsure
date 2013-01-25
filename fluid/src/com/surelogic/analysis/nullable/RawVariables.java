package com.surelogic.analysis.nullable;

import java.util.List;

import com.surelogic.analysis.nullable.RawLattice.Element;
import com.surelogic.util.IRNodeIndexedExtraElementArrayLattice;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Associative array from the receiver declaration (if any), and any non-array
 * object-typed local variable and parameter declarations to the raw state of
 * the referenced object. This array always contains an extra element indexed by
 * "null" whose value is initialized to IMPOSSIBLE. If the value of this last
 * element is ever not IMPOSSIBLE, then the value of the array is non-normative.
 */
public final class RawVariables extends IRNodeIndexedExtraElementArrayLattice<RawLattice, RawLattice.Element> {
  private final Element[] empty;

  private RawVariables(
      final IRNode[] modifiedKeys, final RawLattice lat) {
    super(lat, modifiedKeys);

    // Create a unique reference to the empty value
    empty = createEmptyValue();
//    final int n = modifiedKeys.length;
//    empty = new Element[n];
//    for (int i = 0; i < n-1; i++) empty[i] = RawLattice.NOT_RAW;
//    empty[n-1] = RawLattice.IMPOSSIBLE;
  }

  public static RawVariables create(
      final List<IRNode> keys, final RawLattice lattice) {
    return new RawVariables(modifyKeys(keys), lattice);
  }

  @Override
  public Element[] getEmptyValue() {
    return empty;
  }

  @Override
  protected Element[] newArray() {
    return new Element[size];
  }
  
  @Override
  protected Element getEmptyElementValue() { return RawLattice.NOT_RAW; }
  
  @Override
  protected Element getNormalFlagValue() { return RawLattice.IMPOSSIBLE; }
  
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
