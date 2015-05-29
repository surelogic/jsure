package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.effects.targets.evidence.TargetEvidence;

import edu.cmu.cs.fluid.java.bind.IBinder;

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
  /** 
   * Evidence, if any, for the target's existence.  May be <code>null</code>
   */
  protected final TargetEvidence evidence;
  


  AbstractTarget(final TargetEvidence te) {
    evidence = te;
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
   * Force subclasses to use {@link #toString(StringBuilder)} as 
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
