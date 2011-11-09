package com.surelogic.analysis.effects.targets;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;

public final class QualifiedReceiverConversionEvidence extends CallEvidence {
  private final IRNode origRef;
  private final IJavaReferenceType type;
  
  public QualifiedReceiverConversionEvidence(
      final IRNode m, final IRNode or, final IJavaReferenceType t) {
    super(m);
    origRef = or;
    type = t;
  }

  public IRNode getQualifiedReceiver() { return origRef; }
  public IJavaReferenceType getType() { return type; }
  
  @Override
  public void visit(final EvidenceVisitor v) {
    v.visitQualifiedReceiverConversionEvidence(this);
  }
}
