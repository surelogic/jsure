package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.java.bind.*;

/**
 * Abstract class for representing targets of effects. These are not the same
 * as <em>regions</em>, but they do make use of regions. For example, <tt>InstanceTarget</tt>
 * contains a reference to an object instance and the region of the instance
 * being read/written.
 * 
 * <P>
 * Target objects are immutable.
 *
 * @see Effect
 * @see EmptyTarget
 * @see AnyInstanceTarget
 * @see ClassTarget
 * @see InstanceTarget
 * @see LocalTarget
 */
abstract class AbstractTarget implements Target {
  /** The region accessed by the target */
  protected final IRegion region;

  /** 
   * Evidence, if any, for the target's existence.  May be <code>null</code>
   */
  protected final TargetEvidence evidence;
  


  /** Only for use by LocalTarget and EmptyTarget. */
  AbstractTarget(final TargetEvidence te) {
    region = null;
    evidence = te;
  }

  protected AbstractTarget(final IRegion reg, final TargetEvidence te) {
    if (reg == null) {
      throw new NullPointerException("region cannot be null");
    }
    region = reg;
    evidence = te;
  }
  
    
  
  /**
   * Get the region component of the target.
   * 
   * @return The region component
   */
  @Override
  public final IRegion getRegion() {
    return region;
  }

  // Used by implementations of degradeRegion()
  protected final void checkNewRegion(final IRegion newRegion) {
    if (!newRegion.ancestorOf(region)) {
      throw new IllegalArgumentException("New region is not an ancestor of the old region");
    }
  }

  
  
  // Used by implementations of the overlapsWith methods
  /**
   * Returns whether <code>t1</code> is an ancestor of <code>t2</code>,
   * or vice versa.  This uses ITypeEnvironment.isSubType() which 
   * already does the right thing for related IJavaArrayType to IJavaDeclaredType.
   * (This is, arrays are subtypes of java.lang.Object.)
   */
  static boolean areDirectlyRelated(
    final IBinder b, final IJavaType t1, final IJavaType t2) {
    ITypeEnvironment tEnv = b.getTypeEnvironment();
    return tEnv.isRawSubType(t1, t2) || tEnv.isRawSubType(t2, t1);
  }

  /* For double dispatching in the implementation of overlapsWith() */  

  // Receiver is the argument from the original overlapsWith() call
  abstract TargetRelationship overlapsWithEmpty(IBinder binder, EmptyTarget t);

  // Receiver is the argument from the original overlapsWith() call
  abstract TargetRelationship overlapsWithLocal(IBinder binder, LocalTarget t);

  // Receiver is the argument from the original overlapsWith() call
  abstract TargetRelationship overlapsWithAnyInstance(
      IBinder binder, AnyInstanceTarget t);

  // Receiver is the argument from the original overlapsWith() call
  abstract TargetRelationship overlapsWithClass(
      IBinder binder, ClassTarget t);

  // Receiver is the argument from the original overlapsWith() call
  abstract TargetRelationship overlapsWithInstance(
      IMayAlias mayAlias, IBinder binder, InstanceTarget t);

  

  // Used by implementations of checkTarget methods
  /**
   * Returns whether <code>t1</code> is an ancestor of <code>t2</code>.
   * This uses ITypeEnvironment.isSubType() which 
   * already does the right thing for related IJavaArrayType to IJavaDeclaredType.
   * (This is, arrays are subtypes of java.lang.Object.)
   */
  static boolean isAncestorOf(
    final IBinder b, final IJavaType t1, final IJavaType t2) {
    ITypeEnvironment tEnv = b.getTypeEnvironment();
    return tEnv.isRawSubType(t2, t1);
  }

  /* For double dispatching in the implementation of checkTarget() */
  
  // Receiver is the target from the declared effect
  abstract boolean checkTargetAgainstEmpty(IBinder b, EmptyTarget actualTarget);
  
  // Receiver is the target from the declared effect
  abstract boolean checkTargetAgainstLocal(IBinder b, LocalTarget actualTarget);
  
  // Receiver is the target from the declared effect
  abstract boolean checkTargetAgainstAnyInstance(IBinder b, AnyInstanceTarget actualTarget);
  
  // Receiver is the target from the declared effect
  abstract boolean checkTargetAgainstClass(IBinder b, ClassTarget actualTarget);
  
  // Receiver is the target from the declared effect
  abstract boolean checkTargetAgainstInstance(IBinder b, InstanceTarget actualTarget);

  
  
  @Override
  public final TargetEvidence getEvidence() {
    return evidence;
  }


  
  /**
   * Force subclasses to get used {@link #toString(StringBuilder)} as 
   * the implementation.
   */
  @Override
  public final String toString() {
    return toString(new StringBuilder()).toString();
  }

  /**
	 * Make equals abstract so that the subclasses will be forced to implement
	 * it. We want them to do this because Targets are immutable.
	 */
  @Override
  public abstract boolean equals(Object o);

  /**
	 * Make hashCode abstract so that the subclasses will be forced to implement
	 * it. We want them to do this because Targets are immutable.
	 */
  @Override
  public abstract int hashCode();
}
