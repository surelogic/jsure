package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.effects.targets.evidence.TargetEvidence;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;

/**
 * Represents no state at all.  An effect on an empty target does nothing.
 * Used to report non-effects to the user that original due to reads of final
 * fields, or uses of immutable references, etc.
 * 
 * <p>An empty target is checked by any other target, but overlaps with
 * no target, as it represents nothing. 
 */
public final class EmptyTarget extends AbstractTarget {
  public EmptyTarget(final TargetEvidence ee) {
    super(ee);
  }
  
  
  
  @Override
  public IRNode getReference() {
    return null;
  }
  
  @Override
  public final IRegion getRegion() {
    return null;
  }
  
  
  
  @Override
  public IJavaType getRelativeClass(final IBinder binder) {
    return null;
  }
  
  @Override
  public Target degradeRegion(final IRegion newRegion) {
    // doesn't use the region, so return self
    return this;
  }

  
  
  @Override
  public Target mask(final IBinder binder) {
    // We want this to percolate up to the results, so never mask them
    return this;
  }

  @Override
  public boolean overlapsReceiver(final IRNode rcvrNode) {
    return false;
  }

  @Override
  public TargetRelationship overlapsWith(
      final IMayAlias mayAlias, final IBinder binder, final Target t) {
    return ((AbstractTarget) t).overlapsWithEmpty(binder, this);
  }

  @Override
  // T is the receiver in the original overlapsWIth() call!
  TargetRelationship overlapsWithEmpty(final IBinder binder, final EmptyTarget t) {
    return TargetRelationship.unrelated();
  }
  
  // t is the receiver, and thus TARGET A, in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithLocal(
      final IBinder binder, final LocalTarget t) {
    return TargetRelationship.unrelated();
  }

  // t is the receiver, and thus TARGET A, in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithAnyInstance(
      final IBinder binder, final AnyInstanceTarget t) {
    return TargetRelationship.unrelated();
  }

  // t is the receiver, and thus TARGET A, in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithClass(
      final IBinder binder, final ClassTarget t) {
    return TargetRelationship.unrelated();
  }

  // t is the receiver, and thus TARGET A, in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithInstance(
      final IMayAlias mayAlias, final IBinder binder, final InstanceTarget t) {
    return TargetRelationship.unrelated();
  }

  @Override
  public boolean mayTargetStateOfReference(
      final IBinder binder, final IRNode formal) {
    return false;
  }


  
  @Override
  public boolean checkTarget(final IBinder b, final Target declaredTarget) {
    return ((AbstractTarget) declaredTarget).checkTargetAgainstEmpty(b, this);
  }
  
  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstEmpty(
      final IBinder b, final EmptyTarget actualTarget) {
    return true;
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
    return false;
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
    return false;
  }
  
  
  
  @Override
  public EmptyTarget changeEvidence(final TargetEvidence e) {
    return new EmptyTarget(e);
  }
  
  
  
  @Override
  public StringBuilder toString(final StringBuilder sb) {
    sb.append("nothing");
    return sb;
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof EmptyTarget) {
      final EmptyTarget t = (EmptyTarget) o;
      return (evidence == null ? t.evidence == null : evidence.equals(t.evidence));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + ((evidence == null) ? 0 : evidence.hashCode());
    return result;
  }
}
