package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.effects.targets.evidence.TargetEvidence;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.regions.RegionRelationships;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.util.TypeUtil;

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
public final class AnyInstanceTarget extends AbstractTargetWithRegion {
  /**
   * Reference to the class declaration node of the class that parameterizes the
   * target.
   */
  final IJavaReferenceType clazz;  
  
  
  
  /**
   * Create a new target parameterized by the given class and referring to the
   * given region.
   */
  // Force use of the target factories
  AnyInstanceTarget(final IJavaReferenceType c, final IRegion r,
      final TargetEvidence te) {
    super(r, te);
    
    // Region cannot be static: use class target
    if (r.isStatic()) {
      throw new IllegalArgumentException("Region cannot be static: use a ClassTarget instead");
    }
    if (c == null) {
      throw new IllegalArgumentException("The class parameter is null");
    } else if (!(c instanceof IJavaDeclaredType) && !(c instanceof IJavaArrayType)) {
      throw new IllegalArgumentException("The class parameter must be an IJavaDeclaredType or an IJavaArrayType, not a " + c.getClass().getName());
    }
    clazz = c;
  }

  
  
  @Override
  public IRNode getReference() {
    return null;
  }
  
  @Override
  public IJavaType getRelativeClass(final IBinder binder) {
    return clazz;
  }

  @Override
  public Target degradeRegion(final IRegion newRegion) {
    checkNewRegion(newRegion);
    if (newRegion.isStatic()) {
      return new ClassTarget(newRegion, evidence);
    } else {
      return new AnyInstanceTarget(clazz, newRegion, evidence);
    }
  }

  
  
  public boolean isMaskable(final IBinder binder) {
    // Any instance targets are never maskable
    return false;
  }

  @Override
  public Target mask(final IBinder binder) {
    // Any instance targets are never maskable
    return this;
  }

  
  
  @Override
  public boolean overlapsReceiver(final IRNode rcvrNode) {
    return false;
  }

  @Override
  public TargetRelationship overlapsWith(
      final IMayAlias mayAlias, final IBinder binder, final Target t) {
    return ((AbstractTarget) t).overlapsWithAnyInstance(binder, this);
  }


  // t is the receiver, and thus TARGET A, in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithEmpty(final IBinder binder, final EmptyTarget t) {
    return TargetRelationship.unrelated();
  }

  // t is the receiver, and thus TARGET A, in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithLocal(final IBinder binder, final LocalTarget t) {
    return TargetRelationship.unrelated();
  }

  // t is the receiver, and thus TARGET A, in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithAnyInstance(
      final IBinder binder, final AnyInstanceTarget t) {
    final IRegion regionA = t.region;
    final IJavaReferenceType classA = t.clazz;
    final IRegion regionB = this.region;
    final IJavaType classB = this.clazz;
    if (TypeUtil.areDirectlyRelated(binder.getTypeEnvironment(), classB, classA)) {
      if (regionA.equals(regionB)) {
        return TargetRelationship.aliased(RegionRelationships.EQUAL);
      } else if (regionA.ancestorOf(regionB)) {
        return TargetRelationship.aliased(
          RegionRelationships.REGION_A_INCLUDES_REGION_B);
      } else if (regionB.ancestorOf(regionA)) {
        return TargetRelationship.aliased(
          RegionRelationships.REGION_B_INCLUDES_REGION_A);
      }
    }
    return TargetRelationship.unrelated();
  }

  // t is the receiver, and thus TARGET A, in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithClass(
      final IBinder binder, final ClassTarget t) {
    final IRegion regionA = t.region;
    final IRegion regionB = this.region;
    if (regionA.equals(regionB)) {
      // Shouldn't happen
      throw new IllegalStateException("Region in Class target equal to region in AnyInstance target!");
    } else if (regionA.ancestorOf(regionB)) {
      return TargetRelationship.aIsLarger(
        RegionRelationships.REGION_A_INCLUDES_REGION_B);
    } else if (regionB.ancestorOf(regionA)) {
      // shouldn't happen
      throw new IllegalStateException("Region in AnyInstace target contains the region in the Class target!");
    } else {
      return TargetRelationship.unrelated();
    }
  }

  // t is the receiver, and thus TARGET A, in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithInstance(
      final IMayAlias mayAlias, final IBinder binder, final InstanceTarget t) {
    /* NB. page 229 of ECOOP paper says we should check that Instance target
     * is shared (!unique). I think this because we want to make sure that
     * overlap is based on the aggregated region hierarchy. We don't have to
     * check this here because we are assuming that effects have already
     * been elaborated and masked, and thus aggregation relationships have
     * already been resolved.
     */
    if (TypeUtil.areDirectlyRelated(binder.getTypeEnvironment(), binder.getJavaType(t.reference), this.clazz)) {
      final IRegion regionA = t.region;
      final IRegion regionB = this.region;
      if (regionA.equals(regionB)) {
        return TargetRelationship.bIsLarger(RegionRelationships.EQUAL);
      } else if (regionA.ancestorOf(regionB)) {
        return TargetRelationship.aliased(
          RegionRelationships.REGION_A_INCLUDES_REGION_B);
      } else if (regionB.ancestorOf(regionA)) {
        return TargetRelationship.bIsLarger(
          RegionRelationships.REGION_B_INCLUDES_REGION_A);
      }
    }
    return TargetRelationship.unrelated();
  }

  @Override
  public boolean mayTargetStateOfReference(
      final IBinder binder, final IRNode formal) {
    return TypeUtil.areDirectlyRelated(binder.getTypeEnvironment(), binder.getJavaType(formal), this.clazz);
  }

  
  
  @Override
  public boolean checkTarget(final IBinder b, final Target declaredTarget) {
    return ((AbstractTarget) declaredTarget).checkTargetAgainstAnyInstance(b, this);
  }

  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstEmpty(
      final IBinder b, final EmptyTarget actualTarget) {
    return false;
  }

  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstLocal(
      final IBinder b, final LocalTarget actualTarget) {
    return false;
  }

  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstAnyInstance(
      final IBinder b, final AnyInstanceTarget actualTarget) {
    return TypeUtil.isAncestorOf(b.getTypeEnvironment(), this.clazz, actualTarget.clazz)
        && this.getRegion().ancestorOf(actualTarget.region);
  }

  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstClass(
      final IBinder b, final ClassTarget actualTarget) {
   return false;
  }

  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstInstance(
      final IBinder b, final InstanceTarget actualTarget) {
    final IJavaType clazz = b.getJavaType(actualTarget.reference);
    return TypeUtil.isAncestorOf(b.getTypeEnvironment(), this.clazz, clazz)
        && this.region.getRegion().ancestorOf(actualTarget.region);
  }

 
  
  @Override
  public AnyInstanceTarget changeEvidence(final TargetEvidence e) {
    return new AnyInstanceTarget(clazz, region, e);
  }
  
  

  @Override
  public StringBuilder toString(final StringBuilder sb) {
    sb.append("any("); 
    if (clazz instanceof IJavaDeclaredType) {
      // Cannot use clazz.getName because it includes type parameters which are not allowed in RegionEffects declarations.
      sb.append(JavaNames.getFullTypeName(((IJavaDeclaredType) clazz).getDeclaration()));
    } else { // IJavaArrayType
      sb.append(clazz.getName());
    }
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
      return clazz.equals(t.clazz)
          && region.equals(t.region)
          && (evidence == null ? t.evidence == null : evidence.equals(t.evidence));
          
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
    int result = 17;
    result = 31 * result + clazz.hashCode();
    result = 31 * result + region.hashCode();
    result = 31 * result + (evidence == null ? 0 : evidence.hashCode());
    return result;
  }
}
