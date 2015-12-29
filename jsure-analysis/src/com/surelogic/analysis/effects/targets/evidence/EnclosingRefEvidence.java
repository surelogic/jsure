package com.surelogic.analysis.effects.targets.evidence;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;

public final class EnclosingRefEvidence implements TargetEvidence {
  private final IRNode link;
  private final IRNode original;
  private final IRNode enclosingRef;
  
  public EnclosingRefEvidence(final IRNode link, final IRNode original, final IRNode enclosingRef) {
    this.link = link;
    this.original = original;
    this.enclosingRef = enclosingRef;
  }

  public IRNode getOriginal() {
    return original;
  }
  
  public IRNode getEnclosingRef() {
    return enclosingRef;
  }
  
  @Override
  public IRNode getLink() {
    return link;
  }

  @Override
  public void visit(final EvidenceVisitor v) {
    v.visitEnclosingRefEvidence(this);
  }

  public static String unparseRef(final IRNode ref) {
    if (ParameterDeclaration.prototype.includes(ref)) {
      return ParameterDeclaration.getId(ref);
    } else {
      return DebugUnparser.toString(ref);
    }
  }
}
