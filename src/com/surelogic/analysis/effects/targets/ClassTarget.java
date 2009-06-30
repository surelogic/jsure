package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.regions.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/*
 * 99 Feb 23 Remove iwAnything() because I removed the AnythingTarget class.
 * Added implementation of equals() and hashCode()
 */

/*
 * 98 Sept 11 Removed iwArrayElt because I removed the ArrayEltTarget class
 */

/**
 * <em>These Target classes are a disaster.  They need to be redone in a more
 * understandable way.  I've spent the last 8 years trying to make them 
 * make sense, I don't really feel like I've succeeded.  Someone who is smarter 
 * than I am needs to fix this.  The problem is that Java has very bad
 * support for union types.</em> &mdash; Aaron Greenhouse, 18 Oct 2006.
 */
/* I only want this class to be usable by the TargetFactory implementations */
public final class ClassTarget extends AbstractTarget {
  // Force use of the target factories
  ClassTarget(IRegion rgn) {
    super(rgn);
  }

  @Override
  public Kind getKind() {
    return Target.Kind.CLASS_TARGET;
  }
  
  public boolean isMaskable(final IBinder binder) {
    // Class targets are never maskable
    return false;
  }

  public boolean overlapsReceiver(final IRNode rcvrNode) {
    return false;
  }

  /**
	 * A static target will check against another static target whose region is
	 * an ancestor of its own region.
	 */
  public boolean checkTgt(final IBinder b, final Target t) {
    if (t.getKind() == Target.Kind.CLASS_TARGET) {
      if (t.getRegion().ancestorOf(region)) {
        return true;
      }
    }
    return false;
  }

  @Override TargetRelationship owLocal(final LocalTarget t) {
    return TargetRelationship.newUnrelated();
  }

  @Override TargetRelationship owAnyInstance(
    final IBinder binder, final IJavaType c, final IRegion reg) {
    if (region.equals(reg)) {
      return TargetRelationship.newAIsLarger(RegionRelationships.EQUAL);
    } else if (region.ancestorOf(reg)) {
      return TargetRelationship.newAIsLarger(
        RegionRelationships.REGION_A_INCLUDES_REGION_B);
    } else if (reg.ancestorOf(region)) {
      return TargetRelationship.newAIsLarger(
        RegionRelationships.REGION_B_INCLUDES_REGION_A);
    } else {
      return TargetRelationship.newUnrelated();
    }
  }

  @Override TargetRelationship owClass(final IBinder binder, final IRegion reg) {
    if (region.equals(reg)) {
      return TargetRelationship.newAliased(RegionRelationships.EQUAL);
    } else if (region.ancestorOf(reg)) {
      return TargetRelationship.newAIsLarger(
        RegionRelationships.REGION_A_INCLUDES_REGION_B);
    } else if (reg.ancestorOf(region)) {
      return TargetRelationship.newBIsLarger(
        RegionRelationships.REGION_B_INCLUDES_REGION_A);
    } else {
      return TargetRelationship.newUnrelated();
    }
  }

  @Override TargetRelationship owInstance(
    final IAliasAnalysis.Method am, final IBinder binder, final IRNode ref, final IRegion reg) {
    /* NB. page 229 of ECOOP paper says we should check that Instance target
     * is shared (!unique). I think this because we want to make sure that
     * overlap is based on the aggregated region hierarchy. We don't have to
     * check this here because we are assuming that effects have already
     * been elaborated and masked, and thus aggregation relationships have
     * already been resolved.
     */
    if (region.equals(reg)) {
      // Should never happen???
      LOG.warning("Region in Class target equal to region in Instance target!");
      return TargetRelationship.newAIsLarger(RegionRelationships.EQUAL);
    } else if (region.ancestorOf(reg)) {
      return TargetRelationship.newAIsLarger(
        RegionRelationships.REGION_A_INCLUDES_REGION_B);
    } else if (reg.ancestorOf(region)) {
      return TargetRelationship.newAIsLarger(
        RegionRelationships.REGION_B_INCLUDES_REGION_A);
    } else {
      return TargetRelationship.newUnrelated();
    }
  }

  @Override
  public StringBuilder toString(final StringBuilder sb) {
    sb.append(
        JavaNames.getQualifiedTypeName(
            VisitUtil.getClosestType(region.getNode())));
    sb.append(':');
    sb.append(region.getName());
    return sb;
  }

  /**
	 * Compare two class targets. Two local targets are equal if the refer to the
	 * same region.
	 */
  @Override
  public boolean equals(final Object o) {
    if (o instanceof ClassTarget) {
      final ClassTarget t = (ClassTarget) o;
      return region.equals(t.region);
    }
    return false;
  }

  /**
	 * Get the hashcode of a class target.
	 * 
	 * @return The hashcode, which is the hashcode of the region.
	 */
  @Override
  public int hashCode() {
    return region.hashCode();
  }
}
