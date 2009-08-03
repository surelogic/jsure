/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/AggregationEvidence.java,v 1.2 2008/01/19 00:14:21 aarong Exp $*/
package com.surelogic.analysis.locks;

import java.util.Set;

import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.locks.locks.NeededLock;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;

@Deprecated
final class AggregationEvidence {
  public final IRNode originalObjExpr;
  public final IRegion originalRegion;
  public final Set<Effect> conflicts;
  public final IRegion mappedRegion;
  public final NeededLock neededLock;
  
  public AggregationEvidence(
      final IRNode originalObjExpr, final IRegion originalRegion,
      final Set<Effect> conflicts, final IRegion mappedRegion,
      final NeededLock neededLock) {
    this.originalObjExpr = originalObjExpr;
    this.originalRegion = originalRegion;
    this.conflicts = conflicts;
    this.mappedRegion = mappedRegion;
    this.neededLock = neededLock;
  }

}
