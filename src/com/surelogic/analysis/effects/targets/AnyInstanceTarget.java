package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.regions.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/*
 * 99 Feb 23 Remove iwAnything() because I removed the AnythingTarget class.
 * Added implementation of equals() and hashCode()
 */

/*
 * 98 Sept 11 Removed iwArrayElt because I removed the ArrayEltTarget class
 */

/*
 * 98-06-01: - Added javadoc
 */

/**
 * This class represents a target that accesses a region <i>r</i> of <em>any</em>
 * instance of a given class <i>C</i> or subclasses of <i>C</i>. It will
 * intersect with any target that accesses a region <i>r'</i> of an instance
 * of class <i>C</i> or a subclass of <i>C</i>, where <i>r'</i> may
 * intersect with <i>r</i>.
 * 
 * <em>These Target classes are a disaster.  They need to be redone in a more
 * understandable way.  I've spent the last 8 years trying to make them 
 * make sense, I don't really feel like I've succeeded.  Someone who is smarter 
 * than I am needs to fix this.  The problem is that Java has very bad
 * support for union types.</em> &mdash; Aaron Greenhouse, 18 Oct 2006.
 * 
 * @author Aaron Greenhouse
 */
// TODO: Need to deal with the class parameter of the any-instance target
/* I only want this class to be usable by the TargetFactory implementations */
public final class AnyInstanceTarget extends AbstractTarget {
  /**
   * Reference to the class declaration node of the class that parameterizes the
   * target.
   */
  // Sleazy: Need to refactor these classes yet again!
  final IJavaReferenceType clazz;  
  
  
  /**
   * Create a new target parameterized by the given class and referring to the
   * given region.
   */
  // Force use of the target factories
  AnyInstanceTarget(final IJavaReferenceType c, final IRegion r) {
    super(r);
    if (c == null ) {
      throw new IllegalArgumentException("The class parameter is null");
    }
    else if (!(c instanceof IJavaDeclaredType) && !(c instanceof IJavaArrayType)) {
      throw new IllegalArgumentException("The class parameter must be a IJavaDeclaredType or an IJavaArrayType, not a " + c.getClass().getName());
    }
    clazz = c;
  }

  public final boolean isArray() {
    return clazz instanceof IJavaArrayType;
  }
  
  @Override
  public Kind getKind() {
    return Target.Kind.ANY_INSTANCE_TARGET;
  }
  
  public boolean isMaskable(final IBinder binder) {
    // Any instance targets are never maskable
    return false;
  }

  public boolean overlapsReceiver(final IRNode rcvrNode) {
    return false;
  }

  /**
   * An any instance target will check against a static target whose region
   * includes the region named in the any instance target, and will also check
   * against a any instance target (1) whose region includes the region named in
   * this target, and (2) whose class parameter is direct descendant or a direct
   * ancestor of this target's class parameter.
   */
  public boolean checkTgt(final IBinder b, final Target t) {
    if (t.getKind() == Target.Kind.CLASS_TARGET) {
      if (t.getRegion().ancestorOf(region)) {
        return true;
      }
    } else if (t.getKind() == Target.Kind.ANY_INSTANCE_TARGET) {
      final AnyInstanceTarget other = (AnyInstanceTarget) t;
      if (areDirectlyRelated(b, clazz, other.clazz)
        && other.getRegion().ancestorOf(region)) {
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
    if (areDirectlyRelated(binder, c, clazz)) {
      if (region.equals(reg)) {
        return TargetRelationship.newAliased(RegionRelationships.EQUAL);
      } else if (region.ancestorOf(reg)) {
        return TargetRelationship.newAliased(
          RegionRelationships.REGION_A_INCLUDES_REGION_B);
      } else if (reg.ancestorOf(region)) {
        return TargetRelationship.newAliased(
          RegionRelationships.REGION_B_INCLUDES_REGION_A);
      }
    }
    return TargetRelationship.newUnrelated();
  }

  @Override TargetRelationship owClass(final IBinder binder, final IRegion reg) {
    if (region.equals(reg)) {
      return TargetRelationship.newBIsLarger(RegionRelationships.EQUAL);
    } else if (region.ancestorOf(reg)) {
      return TargetRelationship.newBIsLarger(
        RegionRelationships.REGION_A_INCLUDES_REGION_B);
    } else if (reg.ancestorOf(region)) {
      return TargetRelationship.newBIsLarger(
        RegionRelationships.REGION_B_INCLUDES_REGION_A);
    } else {
      return TargetRelationship.newUnrelated();
    }
  }

  @Override TargetRelationship owInstance(
    final IAliasAnalysis.Method am, final IBinder binder, final IRNode ref,
    final IRegion reg) {
    /* NB. Page 229 of ECOOP paper says we should check that Instance target is
     * shared (!unique). I think this because we want to make sure that
     * overlap is based on the aggregated region hierarchy. We don't have to
     * check this here because we are assuming that effects have already been
     * elaborated and masked, and thus aggregation relationships have already
     * been resolved.
     */
    if (areDirectlyRelated(binder, binder.getJavaType(ref), clazz)) {
      if (region.equals(reg)) {
        return TargetRelationship.newAliased(RegionRelationships.EQUAL);
      } else if (region.ancestorOf(reg)) {
        return TargetRelationship.newAliased(
          RegionRelationships.REGION_A_INCLUDES_REGION_B);
      } else if (reg.ancestorOf(region)) {
        return TargetRelationship.newAliased(
          RegionRelationships.REGION_B_INCLUDES_REGION_A);
      }
    }
    return TargetRelationship.newUnrelated();
  }

  @Override
  public StringBuilder toString(final StringBuilder sb) {
    sb.append("any(");
    sb.append(clazz.getName());
    sb.append("):");
    sb.append(region.getName()); // XXX: Doesn't handle region shadowing well
    return sb;
  }

  /**
   * Compare two any instance targets.  They are equal if they refer to the same
   * region and have the same class qualifier.
   */
  @Override
  public boolean equals(final Object o) {
    if (o instanceof AnyInstanceTarget) {
      final AnyInstanceTarget t = (AnyInstanceTarget) o;
      return clazz.equals(t.clazz) && region.equals(t.region);
    }
    return false;
  }

  /**
   * Get the hashcode of a any instance target.
   * 
   * @return The hashcode, which is the sum of the hashcode of the type and the
   *         hashcode of the region.
   */
  @Override
  public int hashCode() {
    return clazz.hashCode() + region.hashCode();
  }
}
