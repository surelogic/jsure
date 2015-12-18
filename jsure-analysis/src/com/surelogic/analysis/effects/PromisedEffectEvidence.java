package com.surelogic.analysis.effects;

import com.surelogic.dropsea.ir.drops.method.constraints.RegionEffectsPromiseDrop;

/**
 * Evidence that the effect comes from getting the declared effects of a method
 * and that the method explicitly declares effects.
 */
public final class PromisedEffectEvidence implements EffectEvidence {
  private final RegionEffectsPromiseDrop promiseDrop;
  
  public PromisedEffectEvidence(final RegionEffectsPromiseDrop promiseDrop) {
    this.promiseDrop = promiseDrop;
  }

  public RegionEffectsPromiseDrop getPromiseDrop() {
    return promiseDrop;
  }

  
  
  @Override
  public void visit(final EffectEvidenceVisitor visitor) {
    visitor.visitPromisedEffectEvidence(this);
  }
  
  @Override
  public int hashCode() {
    int result = 17;
    result += 31 * result + promiseDrop.hashCode();
    return result;
  }
  
  @Override
  public boolean equals(final Object other) { 
    if (other == this) {
      return true;
    } else if (other instanceof PromisedEffectEvidence) {
      final PromisedEffectEvidence o2 = (PromisedEffectEvidence) other;
      return this.promiseDrop.equals(o2.promiseDrop);
    }
    return false;
  }
}
