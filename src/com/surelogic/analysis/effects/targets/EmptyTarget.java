package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.effects.BCAEvidence;
import com.surelogic.analysis.effects.ElaborationEvidence;
import com.surelogic.analysis.effects.Messages;
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
  public enum Reason {
    DECLARES_NO_EFFECTS(Messages.REASON_NO_DECLARED_EFFECT),
    RECEIVER_IS_IMMUTABLE(Messages.REASON_RECEIVER_IS_IMMUTABLE);
    
    private int msg;

    private Reason(final int m) {
      msg = m;
    }
    
    public int getMessage() {
      return msg;
    }
  }
  
  
  
  private final ElaborationEvidence elabEvidence;
  
  private final Reason reason; 
  
  
  
  // Force use of the target factories
  EmptyTarget(final ElaborationEvidence ee, final Reason r) {
    super();
    elabEvidence = ee;
    reason = r;
  }
  
  public Reason getReason() {
    return reason;
  }
  
  public Target degradeRegion(final IRegion newRegion) {
    // doesn't use the region, so return self
    return this;
  }
  
  public IJavaType getRelativeClass(final IBinder binder) {
    return null;
  }
  
  public Target undoBCAElaboration() {
    Target current = this;
    ElaborationEvidence ee = current.getElaborationEvidence();
    while (ee instanceof BCAEvidence) { // null never satisfies instanceof
      current = ee.getElaboratedFrom();
      ee = current.getElaborationEvidence();
    }
    return current;
  }
 
  public boolean isMaskable(final IBinder binder) {
    // We want this to percolate up to the results, so never mask them
    return false;
  }

  public boolean overlapsReceiver(final IRNode rcvrNode) {
    return false;
  }

  @Override
  public ElaborationEvidence getElaborationEvidence() {
    return elabEvidence;
  }
  
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
    return true;
  }

  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstAnyInstance(
      final IBinder b, final AnyInstanceTarget actualTarget) {
    return true;
  }

  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstClass(
      final IBinder b, final ClassTarget actualTarget) {
    return true;
  }
  
  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstInstance(
      final IBinder b, final InstanceTarget actualTarget) {
    return true;
  }

  public TargetRelationship overlapsWith(
      final IMayAlias mayAlias, final IBinder binder, final Target t) {
    return ((AbstractTarget) t).overlapsWithEmpty(binder, this);
  }

  @Override
  // T is the receiver in the original overlapsWIth() call!
  TargetRelationship overlapsWithEmpty(final IBinder binder, final EmptyTarget t) {
    return TargetRelationship.newUnrelated();
  }
  
  // t is the receiver, and thus TARGET A, in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithLocal(
      final IBinder binder, final LocalTarget t) {
    return TargetRelationship.newUnrelated();
  }

  // t is the receiver, and thus TARGET A, in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithAnyInstance(
      final IBinder binder, final AnyInstanceTarget t) {
    return TargetRelationship.newUnrelated();
  }

  // t is the receiver, and thus TARGET A, in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithClass(
      final IBinder binder, final ClassTarget t) {
    return TargetRelationship.newUnrelated();
  }

  // t is the receiver, and thus TARGET A, in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithInstance(
      final IMayAlias mayAlias, final IBinder binder, final InstanceTarget t) {
    return TargetRelationship.newUnrelated();
  }

  @Override
  public StringBuilder toString(final StringBuilder sb) {
    sb.append("nothing");
    return sb;
  }

  /**
	 * Compare two instance targets. Two local targets are equal if the refer to
	 * the same expression and same region.
	 */
  @Override
  public boolean equals(final Object o) {
    if (o instanceof EmptyTarget) {
      final EmptyTarget t = (EmptyTarget) o;
      return region.equals(t.region);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + ((reason == null) ? 0 : reason.hashCode());
    return result;
  }
}
