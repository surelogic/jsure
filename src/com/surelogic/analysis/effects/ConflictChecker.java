/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/effects/ConflictChecker.java,v 1.1 2008/01/22 19:04:56 aarong Exp $*/
package com.surelogic.analysis.effects;

import java.util.Set;

import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetRelationships;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * This class encapsulates checking sets of effects for conflicts.  It is
 * parameterized by an alias analysis.
 * @see ConflictingEffects
 */
public final class ConflictChecker {
  private final IBinder binder;
  private final IAliasAnalysis.MethodFactory methodFactory; 
  
  
  
  public ConflictChecker(final IBinder b, final IAliasAnalysis aa, final IRNode flowUnit) {
    binder = b;
    methodFactory = aa.getMethodFactory(flowUnit);
  }
  
  
  
  public ConflictingEffects getMayConflictingEffects(
    final Set<Effect> s1, final Set<Effect> s2, final IRNode before) {
    return new ConflictingEffects(
        methodFactory.getMayAliasMethod(before), binder, s1, s2);
  }

  public boolean mayConflict(
      final Set<Effect> s1, final Set<Effect> s2, final IRNode before) {
    return getMayConflictingEffects(s1, s2, before).conflictsExist();
  }

  public ConflictingEffects getMustConflictingEffects(
    final Set<Effect> s1, final Set<Effect> s2, final IRNode before) {
    return new ConflictingEffects(
        methodFactory.getMustAliasMethod(before), binder, s1, s2);
  }

  public boolean mustConflict(
      final Set<Effect> s1, final Set<Effect> s2, final IRNode before) {
    return getMustConflictingEffects(s1, s2, before).conflictsExist();
  }
  
  public boolean doTargetsOverlap(final Target t1, final Target t2, final IRNode before) {
    return t1.overlapsWith(methodFactory.getMayAliasMethod(before), binder, t2).getTargetRelationship() != TargetRelationships.UNRELATED;
  }
}
