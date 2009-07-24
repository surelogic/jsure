package com.surelogic.analysis.effects.targets;

import java.util.logging.Logger;

import com.surelogic.analysis.effects.AggregationEvidence;
import com.surelogic.analysis.effects.ElaborationEvidence;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
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
 * @see AnyInstanceTarget
 * @see ClassTarget
 * @see InstanceTarget
 * @see LocalTarget
 * @author Aaron Greenhouse
 */
abstract class AbstractTarget implements Target {
  /** Logger instance for debugging. */
  protected static final Logger LOG =
	  SLLogger.getLogger("FLUID.analysis.effects");

  /** The region accessed by the target */
  protected final IRegion region;



  /**
   * Returns whether <code>t1</code> is an ancestor of <code>t2</code>,
   * or vice versa.  This uses ITypeEnvironment.isSubType() which 
   * already does the right thing for related IJavaArrayType to IJavaDeclaredType.
   * (This is, arrays are subtypes of java.lang.Object.)
   */
  static boolean areDirectlyRelated(
    final IBinder b, final IJavaType t1, final IJavaType t2) {
    ITypeEnvironment tEnv = b.getTypeEnvironment();
    return tEnv.isSubType(t1, t2) || tEnv.isSubType(t2, t1);
  }

  
  /** Only for use by LocalTarget. */
  AbstractTarget() {
    region = null;
  }

  protected AbstractTarget(final IRegion reg) {
    if (reg == null) {
      throw new NullPointerException("region cannot be null");
    }
    region = reg;
  }

  // Only instance targets have references
  public IRNode getReference() {
    return null;
  }

  /**
	 * Get the region component of the target.
	 * 
	 * @return The region component
	 */
  public final IRegion getRegion() {
    return region;
  }

  // Only instance or class targets have elaboration evidence
  public ElaborationEvidence getElaborationEvidence() {
    return null;
  }
  
  public boolean isAggregated() {
    final ElaborationEvidence ee = getElaborationEvidence();
    if (ee == null) {
      return false;
    } else if (ee instanceof AggregationEvidence) {
      return true;
    } else {
      return ee.getElaboratedFrom().isAggregated();
    }
  }

  /* For double dispatching in the implementation of checkTarget() */
  
  // Receiver is the target from the declared effect
  abstract boolean checkTargetAgainstLocal(IBinder b, LocalTarget actualTarget);
  
  // Receiver is the target from the declared effect
  abstract boolean checkTargetAgainstAnyInstance(IBinder b, AnyInstanceTarget actualTarget);
  
  // Receiver is the target from the declared effect
  abstract boolean checkTargetAgainstClass(IBinder b, ClassTarget actualTarget);
  
  // Receiver is the target from the declared effect
  abstract boolean checkTargetAgainstInstance(IBinder b, InstanceTarget actualTarget);

  
  
  /* For double dispatching in the implementation of overlapsWith() */  
  
  // Receiver is the argument from the original overlapsWith() call
  abstract TargetRelationship overlapsWithLocal(
      IAliasAnalysis.Method am, IBinder binder, LocalTarget t);

  // Receiver is the argument from the original overlapsWith() call
  abstract TargetRelationship overlapsWithAnyInstance(
      IAliasAnalysis.Method am, IBinder binder, AnyInstanceTarget t);

  // Receiver is the argument from the original overlapsWith() call
  abstract TargetRelationship overlapsWithClass(
      IAliasAnalysis.Method am, IBinder binder, ClassTarget t);

  // Receiver is the argument from the original overlapsWith() call
  abstract TargetRelationship overlapsWithInstance(
      IAliasAnalysis.Method am, IBinder binder, InstanceTarget t);

  
  
  /**
	 * Get the name of the target. This is currently the same as calling <tt>getString()</tt>,
	 * but I'm not yet convinced that I should get rid of it.
	 * 
	 * @return The name of the target, which includes information about the
	 *         embedded region
	 */
  public String getName() {
    return toString();
  }

  @Override
  public final String toString() {
    return toString(new StringBuilder()).toString();
  }
  
  /**
   * Append the string representation of this target to the given 
   * {@code StringBuilder}.
   * @return The string builder passed to {@code sb}.
   */
  public abstract StringBuilder toString(StringBuilder sb);

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
