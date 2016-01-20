package com.surelogic.analysis.effects;

public final class MaskedEffectEvidence implements EffectEvidence {
  private final Effect originalEffect;
  
  public MaskedEffectEvidence(final Effect original) {
    originalEffect = original;
  }

  public Effect getOriginal() {
    return originalEffect;
  }
  
  @Override
  public void visit(final EffectEvidenceVisitor v) {
    v.visitMaskedEffectEvidence(this);
  }
  
  @Override
  public boolean equals(final Object other) {
    if (other == this) {
      return true;
    } else if (other instanceof MaskedEffectEvidence) {
      final MaskedEffectEvidence mee = (MaskedEffectEvidence) other;
      return originalEffect.equals(mee.originalEffect);
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * result + originalEffect.hashCode();
    return result;
  }
}
