package com.surelogic.analysis.effects;

public abstract class EffectEvidenceProcessor implements EffectEvidenceVisitor {
  @Override
  public void accept(final EffectEvidence e) {
    if (e != null) e.visit(this);
  }
  
  public void visit(EffectEvidence e) {
    // do nothing by default
  }
  
  @Override
  public void visitInitializationEvidence(InitializationEvidence e) {
    visit(e);
  }
  
  @Override
  public void visitNoEffectEvidence(NoEffectEvidence e) {
    visit(e);
  }
  
  @Override
  public void visitNoPromisedEffectEvidence(NoPromisedEffectEvidence e) {
    visit(e);
  }
  
  @Override
  public void visitPromisedEffectEvidence(PromisedEffectEvidence e) {
    visit(e);
  }
}
