package com.surelogic.analysis.effects;

/**
 * Evidence that the effect comes from getting the declared effects of a method
 * that doesn't explicitly declare any effects.
 */
public enum NoPromisedEffectEvidence implements EffectEvidence {
  INSTANCE;
  
  @Override
  public void visit(EffectEvidenceVisitor visitor) {
    visitor.visitNoPromisedEffectEvidence(this);
  }
}
