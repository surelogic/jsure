package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;

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
   * Get the class that should be used to look up the region referenced by
   * this target.
   */
  public IJavaType getRelativeClass(IBinder binder);
  
  /**
   * Update the region to the given ancestor region. May change the type of
   * target if the new region is static.
   * 
   * @exception IllegalArgumentException
   *              thrown if <code>newRegion</code> is not an ancestor of the
   *              target's current region.
   */
  public Target degradeRegion(IRegion newRegion);

  
  
  
  /**
   * Mask the target if it refers to state that is not visible outside of the
   * context (i.e., method) in which it originates.
   * @return <code>null</code> if the effect containing the target should be
   * eliminated completely; the receiver if the effect should not be masked; or
   * a new {@link EmptyTarget} if the effect should be masked to an effect
   * on empty state (useful for providing feedback to the end user). 
   */
  public Target mask(IBinder binder);
  
  /**
   * Does the target overlap with the instance region of the given receiver node?
   */
  public boolean overlapsReceiver(IRNode rcvrNode);

  /**
   * Query if two targets overlap, that is, identify potentially overlapping
   * state. This takes static approximations of run-time realities into account,
   * e.g., aliasing.
   */
  public TargetRelationship overlapsWith(
      IMayAlias mayAlias, IBinder binder, Target t);
  
  /**
   * Does an effect on this target have the potential to affect the state of an
   * object referenced by given formal parameter or receiver?
   */
  public boolean mayTargetStateOfReference(IBinder b, IRNode formal);
  
  /**
   * The checkTgt relationship from Chapter 4 of Aaron's dissertation. The
   * receiver must be a target from an effect that has been through elaboration
   * and masking. The parameter <code>declaredTarget</code> must be a target from an
   * effect declared on a method. (This is related to the old includes relationship, but
   * that was semantically suspect, and this operation is more narrowly scoped.)
   * 
   * <p>For <code>x.checkTarget(binder, y)</code> we have that <code>x</code>
   * is a target from an implementation effect, and that <code>y</code> is a 
   * target from a method's declared effects.
   */
  public boolean checkTarget(IBinder b, Target declaredTarget);
  
  
  
  /**
   * Get any additional evidence for why this target exists, and how it may
   * have been computed.  For example region aggregation, binding context
   * information, masking.
   */
  public TargetEvidence getEvidence();
  
  /**
   * Create a new target of the same implementation class and replace the
   * original evidence with the given one.
   */
  public Target changeEvidence(TargetEvidence e);
  
  
  
  /**
   * Add the string representation of the target to the given StringBuilder.
   * 
   * @param sb
   *          The StringBuilder to add the string representation to.
   * @return Returns <code>sb</code>.
   */
  public StringBuilder toString(StringBuilder sb);
}
