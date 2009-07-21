package com.surelogic.analysis.effects.targets;

import java.util.logging.Logger;

import com.surelogic.analysis.regions.IRegion;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.*;

/*
 * 99 Feb 23 Remove iwAnything() because I removed the AnythingTarget class.
 * Made equals() and hashCode() abstract to force the subclasses to implement
 * them.
 */

/*
 * 98 Sept 11 Removed iwArrayElt because I removed the ArrayEltTarget class
 */

/*
 * 98-06-01: - Added intersectsWith() - Added javadoc - really need to unbogufy
 * get/setReference()
 */

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



  /**
   * Returns whether <code>t1</code> is an ancestor of <code>t2</code>,
   * or vice versa.  THis uses ITypeEnvironment.isSubType() which 
   * already does the right thing for related IJavaArrayType to IJavaDeclaredType.
   * (This is, arrays are subtypes of java.lang.Object.)
   */
  static boolean areDirectlyRelated(
    final IBinder b, final IJavaType t1, final IJavaType t2) {
    ITypeEnvironment tEnv = b.getTypeEnvironment();
    return tEnv.isSubType(t1, t2) || tEnv.isSubType(t2, t1);
  }

  

  protected final IRegion region;

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

  /**
	 * Get the reference component of the target (if any).
	 * 
	 * @return The IRNode of the reference component
	 */
  public IRNode getReference() {
    return null;
  }

  /**
	 * Get the region component of the target.
	 * 
	 * @return The region component
	 */
  public IRegion getRegion() {
    return region;
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
  
  abstract TargetRelationship overlapsWithLocal(
      IAliasAnalysis.Method am, IBinder binder, LocalTarget t);

  abstract TargetRelationship overlapsWithAnyInstance(
      IAliasAnalysis.Method am, IBinder binder, AnyInstanceTarget t);

  abstract TargetRelationship overlapsWithClass(
      IAliasAnalysis.Method am, IBinder binder, ClassTarget t);

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
