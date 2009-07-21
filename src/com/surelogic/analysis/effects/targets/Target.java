package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * <em>These Target classes are a disaster.  They need to be redone in a more
 * understandable way.  I've spent the last 8 years trying to make them 
 * make sense, I don't really feel like I've succeeded.  Someone who is smarter 
 * than I am needs to fix this.  The problem is that Java has very bad
 * support for union types.</em>
 * &mdash; Aaron Greenhouse, 18 Oct 2006.
 */
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
  public boolean checkTgt(IBinder b, Target declaredTarget);

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
