package com.surelogic.analysis.effects;

/**
 * A pair of conflicting effects, along with the reasoning
 * behind the conflict. 
 * 
 * @see EffectRelationship
 * @see ConflictingEffects
 */
public final class ConflictingPair {
  /**
   * The first effect of the pair.
   */
  private Effect effectA;

  /**
   * The second effect of the pair
   */
  private Effect effectB;

  /**
   * The reason for the conflict
   */
  private EffectRelationship reason;

  /**
   * Create a new pair.  It should be the case that the two effects conflict.
   * @param a The first effect
   * @param b The second effect
   */
  public ConflictingPair(final Effect a, final Effect b,
      final EffectRelationship rsn) {
    effectA = a;
    effectB = b;
    reason = rsn;
  }

  /**
   * Get the first effect.
   */
  public Effect getEffectA() {
    return effectA;
  }

  /**
   * Get the second effect.
   */
  public Effect getEffectB() {
    return effectB;
  }

  /**
   * Get the explanation for the conflict.  The reason can be
   * tested against the constants above.
   */
  public EffectRelationship getReason() {
    return reason;
  }

  @Override
  public String toString() {
    return effectA + " conflicts with " + effectB + ": " + reason;
  }
}
