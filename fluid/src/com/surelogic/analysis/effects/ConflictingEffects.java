package com.surelogic.analysis.effects;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.surelogic.analysis.alias.IMayAlias;

import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * A class that encapsulates the conflicts between two sets of
 * effects.
 */
public final class ConflictingEffects {
  /**
   * Set of {@link ConflictingPair} instances.  "Effect A" of each pair
   * always comes from {@link #setA}; "effect B" of each pair always
   * comes from {@link #setB}.  If effect X is in setA and effect Y is
   * in setB and X.conflictsWith( Y ) then (X,Y) is in the set.
   */
  private final Set<ConflictingPair> conflictingPairs;

  /**
   * The first set of effects.
   */
  private final Set<Effect> setA;

  /**
   * The second set of effects.
   */
  private final Set<Effect> setB;

  /**
   * Create a new encapsulation of conflicting effects between two sets of
   * effects.
   * @param am The routine to use to check for aliasing among references.
   * @param binder The binder to use
   * @param A The first set of effects.
   * @param B The second set of effects.
   */
  ConflictingEffects(final IMayAlias mayAlias, final IBinder binder,
      final Set<Effect> A, final Set<Effect> B) {
    setA = Collections.unmodifiableSet(A);
    setB = Collections.unmodifiableSet(B);

    final Set<ConflictingPair> pairs = new HashSet<ConflictingPair>();
    for (final Effect effectA : setA) {
      for (final Effect effectB : setB) {
        final EffectRelationship conflict =
          effectA.conflictsWith(mayAlias, binder, effectB);
        if (conflict.isConflict()) {
          pairs.add(new ConflictingPair(effectA, effectB, conflict));
        }
      }
    }

    conflictingPairs = Collections.unmodifiableSet(pairs);
  }

  /** Are there conflicts between the two sets? */
  public boolean conflictsExist() {
    return !conflictingPairs.isEmpty();
  }

  /** Get the first set of effects. */
  public Set<Effect> getSetA() {
    return setA;
  }

  /** Get the second set of effects. */
  public Set<Effect> getSetB() {
    return setB;
  }

  /** Get the set of conflicting pairs of effects. */
  public Set<ConflictingPair> getAsConflictingPairs() {
    return conflictingPairs;
  }

  @Override
  public String toString() {
    return "Conflicts between " + setA + " and " + setB + " are "
        + conflictingPairs;
  }
}
