package com.surelogic.analysis.effects.targets;

import java.util.logging.Logger;

import com.surelogic.analysis.regions.IRegion;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;

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
 * <em>These Target classes are a disaster.  They need to be redone in a more
 * understandable way.  I've spent the last 8 years trying to make them 
 * make sense, I don't really feel like I've succeeded.  Someone who is smarter 
 * than I am needs to fix this.  The problem is that Java has very bad
 * support for union types.</em> &mdash; Aaron Greenhouse, 18 Oct 2006.
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
    final IBinder b,
    final IJavaType t1,
    final IJavaType t2) {
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

  public TargetRelationship overlapsWith(
    final IAliasAnalysis.Method am, final IBinder binder, final Target t) {
    final Kind kind = t.getKind();
    final IRegion reg = t.getRegion();
    IRNode ref = t.getReference();

    if (kind == Target.Kind.LOCAL_TARGET) {
      return owLocal((LocalTarget) t);
    } else if (kind == Target.Kind.INSTANCE_TARGET) {
      return owInstance(am, binder, ref, reg);
    } else if (kind == Target.Kind.CLASS_TARGET) {
      return owClass(binder, reg);
    } else if (kind == Target.Kind.ANY_INSTANCE_TARGET) {
      return owAnyInstance(binder, ((AnyInstanceTarget) t).clazz, reg);
    } else {
      throw new IllegalArgumentException("t is not a known Target kind");
    }
  }

  abstract TargetRelationship owLocal(LocalTarget t);

  abstract TargetRelationship owAnyInstance(
    IBinder binder,
    IJavaType c,
    IRegion reg);

  abstract TargetRelationship owClass(IBinder binder, IRegion reg);

  abstract TargetRelationship owInstance(
      IAliasAnalysis.Method am, IBinder binder, IRNode ref, IRegion reg);

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

  public abstract Kind getKind();

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
