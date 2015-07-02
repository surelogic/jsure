package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.effects.targets.evidence.TargetEvidence;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.regions.RegionRelationships;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * <em>These Target classes are a disaster.  They need to be redone in a more
 * understandable way.  I've spent the last 8 years trying to make them 
 * make sense, I don't really feel like I've succeeded.  Someone who is smarter 
 * than I am needs to fix this.  The problem is that Java has very bad
 * support for union types.</em> &mdash; Aaron Greenhouse, 18 Oct 2006.
 */
/* I only want this class to be usable by the TargetFactory implementations */
public final class ClassTarget extends AbstractTargetWithRegion {
  public ClassTarget(final IRegion rgn, final TargetEvidence evidence) {
    super(rgn, evidence);
  }
  
  
  
  @Override
  public IRNode getReference() {
    return null;
  }
  

  
  @Override
  public IJavaType getRelativeClass(final IBinder binder) {
    final IRNode cdecl = VisitUtil.getClosestType(region.getNode());
    return JavaTypeFactory.getMyThisType(cdecl);
  }

  @Override
  public ClassTarget degradeRegion(final IRegion newRegion) {
    checkNewRegion(newRegion);
    return new ClassTarget(newRegion, evidence);
  }
  
  

  @Override
  public Target mask(final IBinder binder) {
    // class targets are never maskable
    return this;
  }

  
  
  @Override
  public boolean overlapsReceiver(final IRNode rcvrNode) {
    return false;
  }

  @Override
  public TargetRelationship overlapsWith(
      final IMayAlias mayAlias, final IBinder binder, final Target t) {
    return ((AbstractTarget) t).overlapsWithClass(binder, this);
  }

  // t is the receiver, and thus TARGET A, in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithEmpty(final IBinder binder, final EmptyTarget t) {
    return TargetRelationship.unrelated();
  }

  // t is the receiver, and thus TARGET A in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithLocal(final IBinder binder, final LocalTarget t) {
    return TargetRelationship.unrelated();
  }

  // t is the receiver, and thus TARGET A in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithAnyInstance(
      final IBinder binder, final AnyInstanceTarget t) {
    final IRegion regionA = t.region;
    final IRegion regionB = this.region;
    if (regionA.equals(regionB)) {
      // Shouldn't happen
      throw new IllegalStateException("Region in Class target equal to region in AnyInstance target!");
    } else if (regionA.ancestorOf(regionB)) {
      // Shouldn't happen
      throw new IllegalStateException("Region in AnyInstace target contains the region in the Class target!");
    } else if (regionB.ancestorOf(regionA)) {
      return TargetRelationship.bIsLarger(
        RegionRelationships.REGION_B_INCLUDES_REGION_A);
    } else {
      return TargetRelationship.unrelated();
    }
  }

  // t is the receiver, and thus TARGET A in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithClass(
      final IBinder binder, final ClassTarget t) {
    final IRegion regionA = t.region;
    final IRegion regionB = this.region;
    
    if (regionA.equals(regionB)) {
      return TargetRelationship.aliased(RegionRelationships.EQUAL);
    } else if (regionA.ancestorOf(regionB)) {
      return TargetRelationship.aIsLarger(
          RegionRelationships.REGION_A_INCLUDES_REGION_B);
    } else if (regionB.ancestorOf(regionA)) {
      return TargetRelationship.bIsLarger(
          RegionRelationships.REGION_B_INCLUDES_REGION_A);
    } else {
      return TargetRelationship.unrelated();
    }
  }

  // t is the receiver, and thus TARGET A in the original overlapsWith() call!
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
    final IRegion regionA = t.region;
    final IRegion regionB = this.region;
    if (regionA.equals(regionB)) {
      // Shouldn't happen
      throw new IllegalStateException("Region in Class target equal to region in Instance target!");
    } else if (regionA.ancestorOf(regionB)) {
      // SHouldn't happen
      throw new IllegalStateException("Region in Instance target contains the region in the Class target!");
    } else if (regionB.ancestorOf(regionA)) {
      return TargetRelationship.bIsLarger(
        RegionRelationships.REGION_B_INCLUDES_REGION_A);
    } else {
      return TargetRelationship.unrelated();
    }
  }

  @Override
  public boolean mayTargetStateOfReference(
      final IBinder binder, final IRNode formal) {
    /* This is too conservative.  I should really check to see if any of the
     * instance regions in the referenced object are a subregion of the region
     * in this target.
     */
    return TypeUtil.areDirectlyRelated(binder.getTypeEnvironment(), binder.getJavaType(formal), getRelativeClass(binder));
  }

  
  
  @Override
  public boolean checkTarget(final IBinder b, final Target declaredTarget) {
    return ((AbstractTarget) declaredTarget).checkTargetAgainstClass(b, this);
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
    return this.getRegion().ancestorOf(actualTarget.region);
  }
  
  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstClass(
      final IBinder b, final ClassTarget actualTarget) {
    return this.region.ancestorOf(actualTarget.region);
  }
  
  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstInstance(
      final IBinder b, final InstanceTarget actualTarget) {
    return this.region.ancestorOf(actualTarget.region);
  }

  
  
  @Override
  public ClassTarget changeEvidence(final TargetEvidence e) {
    return new ClassTarget(region, e);
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
      return region.equals(t.region) && 
          (evidence == null ? t.evidence == null : evidence.equals(t.evidence));
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
    int result = 17;
    result = 31 * result + region.hashCode();
    result = 31 * result + (evidence == null ? 0 : evidence.hashCode());
    return result;
  }
}
