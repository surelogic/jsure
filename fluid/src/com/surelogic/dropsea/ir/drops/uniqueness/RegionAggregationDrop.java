package com.surelogic.dropsea.ir.drops.uniqueness;

import java.util.Map;

import com.surelogic.analysis.regions.IRegion;
import com.surelogic.dropsea.ir.drops.RegionModel;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * All promises that map state from a reference object into the state of 
 * the referring object implement this interface.
 */
public interface RegionAggregationDrop {
  /**
   * Get the region aggregation mapping declared by this promise drop.
   * 
   * @param fieldDecl
   *          The declaration of the field that is annotated by this drop.
   * @return Map from {@link RegionModel} to {@link Region}.
   */
  public Map<IRegion, IRegion> getAggregationMap(IRNode fieldDecl);
}
