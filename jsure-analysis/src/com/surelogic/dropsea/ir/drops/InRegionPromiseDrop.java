package com.surelogic.dropsea.ir.drops;

import com.surelogic.aast.promise.InRegionNode;
import com.surelogic.dropsea.ir.PromiseDrop;

/**
 * Promise drop for "InRegion" promise annotations defining a region.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 */
public final class InRegionPromiseDrop extends PromiseDrop<InRegionNode> {

  public InRegionPromiseDrop(InRegionNode n) {
    super(n);
  }

  /**
   * Region definitions are not checked by analysis (other than the promise
   * scrubber).
   */
  @Override
  public boolean isIntendedToBeCheckedByAnalysis() {
    return false;
  }
}