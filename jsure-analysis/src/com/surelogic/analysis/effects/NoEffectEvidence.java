package com.surelogic.analysis.effects;

/**
 * Evidence for the case that there is no additional interesting information
 * about the effect.  This is the usual case.
 */
public enum NoEffectEvidence implements EffectEvidence {
  INSTANCE;
  
  @Override
  public void visit(EffectEvidenceVisitor visitor) {
    visitor.visitNoEffectEvidence(this);
  }
}
