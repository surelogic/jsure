package com.surelogic.dropsea.ir.drops.uniqueness;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.surelogic.aast.promise.RegionMappingNode;
import com.surelogic.aast.promise.UniqueMappingNode;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "aggregate" promise annotations defining a region.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 */
public final class ExplicitUniqueInRegionPromiseDrop extends PromiseDrop<UniqueMappingNode> implements RegionAggregationDrop,
    IUniquePromise {

  public ExplicitUniqueInRegionPromiseDrop(UniqueMappingNode n) {
    super(n);
    setCategorizingMessage(JavaGlobals.REGION_CAT);
  }

  @Override
  public boolean isIntendedToBeCheckedByAnalysis() {
    return true;
  }

  @Override
  public ExplicitUniqueInRegionPromiseDrop getDrop() {
    return this;
  }

  @Override
  public Map<IRegion, IRegion> getAggregationMap(final IRNode fieldDecl) {
    final Map<IRegion, IRegion> aggregationMap = new HashMap<IRegion, IRegion>();
    for (final RegionMappingNode mapping : this.getAAST().getSpec().getMappingList()) {
      aggregationMap.put(mapping.getFrom().resolveBinding().getModel(), mapping.getTo().resolveBinding().getRegion());
    }
    return Collections.unmodifiableMap(aggregationMap);
  }
}