package com.surelogic.analysis.effects;

public interface EffectEvidence {
  /**
   * Accept a visitor.
   */
  public void visit(EffectEvidenceVisitor v);
}
