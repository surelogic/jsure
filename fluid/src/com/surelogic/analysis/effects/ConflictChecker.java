package com.surelogic.analysis.effects;

import java.util.Set;

import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetRelationships;

import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * This class encapsulates checking sets of effects for conflicts.  It is
 * parameterized by an alias analysis.
 * @see ConflictingEffects
 */
public final class ConflictChecker {
  private final IBinder binder;
  private final IMayAlias mayAlias; 
  
  
  
  public ConflictChecker(final IBinder b, final IMayAlias ma) {
    binder = b;
    mayAlias = ma;
  }
  
  
  
  public ConflictingEffects getMayConflictingEffects(
    final Set<Effect> s1, final Set<Effect> s2) {
    return new ConflictingEffects(mayAlias, binder, s1, s2);
  }

  public boolean mayConflict(
      final Set<Effect> s1, final Set<Effect> s2) {
    return getMayConflictingEffects(s1, s2).conflictsExist();
  }
  
  public boolean doTargetsOverlap(final Target t1, final Target t2) {
    return t1.overlapsWith(mayAlias, binder, t2).getTargetRelationship() != TargetRelationships.UNRELATED;
  }
}
