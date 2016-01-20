package com.surelogic.analysis.effects;

public abstract class EffectEvidenceProcessor implements EffectEvidenceVisitor {
  public final void accept(final Iterable<EffectEvidence> ee) {
    for (final EffectEvidence e : ee) {
      this.accept(e);
    }
  }
  
  @Override
  public final void accept(final EffectEvidence e) {
    if (e != null) e.visit(this);
  }
  
  public void visit(EffectEvidence e) {
    // do nothing by default
  }
  
  @Override
  public void visitInitializationEffectEvidence(InitializationEffectEvidence e) {
    visit(e);
  }
  
  @Override
  public void visitMaskedEffectEvidence(MaskedEffectEvidence e) {
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
  
  @Override
  public void visitUnresolveableLocksEffectEvidence(UnresolveableLocksEffectEvidence e) {
    visit(e);
  }
}
