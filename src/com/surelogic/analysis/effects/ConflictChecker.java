/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/effects/ConflictChecker.java,v 1.1 2008/01/22 19:04:56 aarong Exp $*/
package com.surelogic.analysis.effects;

import java.util.Set;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * This class encapsulates checking sets of effects for conflicts.  It is
 * parameterized by an alias analysis.
 * @see ConflictingEffects
 */
public final class ConflictChecker {
  final IBinder binder;
  final IAliasAnalysis aliasAnalysis;
  
  
  
  public ConflictChecker(final IBinder b, final IAliasAnalysis aa) {
    binder = b;
    aliasAnalysis = aa;
  }
  
  
  
  public ConflictingEffects getMayConflictingEffects(
    final Set<Effect> s1, final Set<Effect> s2, final IRNode before) {
    return new ConflictingEffects(
        aliasAnalysis.getMayAliasMethod(before), binder, s1, s2);
  }

  public boolean mayConflict(
      final Set<Effect> s1, final Set<Effect> s2, final IRNode before) {
    return getMayConflictingEffects(s1, s2, before).conflictsExist();
  }

  public ConflictingEffects getMustConflictingEffects(
    final Set<Effect> s1, final Set<Effect> s2, final IRNode before) {
    return new ConflictingEffects(
        aliasAnalysis.getMustAliasMethod(before), binder, s1, s2);
  }

  public boolean mustConflict(
      final Set<Effect> s1, final Set<Effect> s2, final IRNode before) {
    return getMustConflictingEffects(s1, s2, before).conflictsExist();
  }
}
