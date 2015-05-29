package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.effects.targets.evidence.TargetEvidence;
import com.surelogic.analysis.regions.IRegion;

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
abstract class AbstractTargetWithRegion extends AbstractTarget {
  /** The region accessed by the target */
  protected final IRegion region;

  protected AbstractTargetWithRegion(final IRegion reg, final TargetEvidence te) {
	super(te);
    if (reg == null) {
      throw new NullPointerException("region cannot be null");
    }
    region = reg;
  }
  
    
  
  /**
   * Get the region component of the target.
   * 
   * @return The region component
   */
  @Override
  public final IRegion getRegion() {
    return region;
  }

  // Used by implementations of degradeRegion()
  protected final void checkNewRegion(final IRegion newRegion) {
    if (!newRegion.ancestorOf(region)) {
      throw new IllegalArgumentException("New region is not an ancestor of the old region");
    }
  }
}
