package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.effects.Effect;

import edu.cmu.cs.fluid.ir.IRNode;

public class AnonClassEvidence implements TargetEvidence {
  private final Effect originalEffect;
  
  public AnonClassEvidence(final Effect effect) {
    originalEffect = effect;
  }
  
  public Effect getOriginalEffect() {
    return originalEffect;
  }
  
  public IRNode getLink() {
    return originalEffect.getSource();
  }

  public void visit(final EvidenceVisitor v) {
    v.visitAnonClassEvidence(this);
  }

}
