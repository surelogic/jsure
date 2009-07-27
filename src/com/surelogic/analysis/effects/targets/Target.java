package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.effects.ElaborationEvidence;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.IBinder;

public interface Target {
  /**
   * Get the reference component of the target (if any).
   * 
   * @return The IRNode of the reference component
   */
  public IRNode getReference();

  /**
   * Get the region component of the target.
   * 
   * @return The region component
   */
  public IRegion getRegion();

  /**
   * Get the evidence chain describing the targets this target was
   * elaborated from, or {@value null} if this target was not generated
   * from elaboration.
   */
  public ElaborationEvidence getElaborationEvidence();
  
  /** 
   * Does this target result from aggregation of state? 
   */
  public boolean isAggregated();
  
  /**
   * Does the target refer to state that is not visible outside of the context
   * (i.e., method) in which it originates. 
   */
  public boolean isMaskable(IBinder binder);
  
  /**
   * Does the target overlap with the instance region of the given receiver node?
   */
  public boolean overlapsReceiver(IRNode rcvrNode);
  
  /**
   * The checkTgt relationship from Chapter 4 of Aaron's dissertation. The
   * receiver must be a target from an effect that has been through elaboration
   * and masking. The parameter <code>declaredTarget</code> must be a target from an
   * effect declared on a method. (This is related to the old includes relationship, but
   * that was semantically suspect, and this operation is more narrowly scoped.)
   */
  public boolean checkTarget(IBinder b, Target declaredTarget);

  /**
   * Query if two targets overlap, that is, identify potentially overlapping
   * state. This takes static approximations of run-time realities into account,
   * e.g., aliasing.
   */
  public TargetRelationship overlapsWith(
      IAliasAnalysis.Method am, IBinder binder, Target t);

  /**
   * Get the name of the target. This is currently the same as calling
   * <tt>getString()</tt>, but I'm not yet convinced that I should get rid of
   * it.
   * 
   * @return The name of the target, which includes information about the
   *         embedded region
   */
  public String getName();
  
  public StringBuilder toString(StringBuilder sb);
}
