package com.surelogic.analysis.effects.targets.evidence;

import com.surelogic.analysis.effects.Effect;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;

public final class UnknownReferenceConversionEvidence implements TargetEvidence {
  private final Effect effect;
  private final IRNode origRef;
  private final IJavaReferenceType type;

  public UnknownReferenceConversionEvidence(final Effect e,
      final IRNode or, final IJavaReferenceType t) {
    effect = e;
    origRef = or;
    type = t;
  }

  public Effect getEffect() { return effect; }
  public IRNode getUnknownRef() { return origRef; }
  public IJavaReferenceType getType() { return type; }
  
  
  @Override
  public IRNode getLink() {
    return effect.getSource();
  }
  
  @Override
  public void visit(final EvidenceVisitor v) {
    v.visitUnknownReferenceConversionEvidence(this);
  }
}
