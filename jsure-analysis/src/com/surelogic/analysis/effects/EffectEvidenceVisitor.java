package com.surelogic.analysis.effects;

public interface EffectEvidenceVisitor {
  /** 'e' may be null */
  public void accept(EffectEvidence e);

  public void visitInitializationEvidence(InitializationEvidence e);
  public void visitNoEffectEvidence(NoEffectEvidence e);
  public void visitNoPromisedEffectEvidence(NoPromisedEffectEvidence e);
  public void visitPromisedEffectEvidence(PromisedEffectEvidence e);
}
