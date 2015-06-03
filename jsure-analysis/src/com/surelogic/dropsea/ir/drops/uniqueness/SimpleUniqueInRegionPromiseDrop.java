package com.surelogic.dropsea.ir.drops.uniqueness;

import java.util.Collections;
import java.util.Map;

import com.surelogic.aast.promise.UniqueInRegionNode;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.IDerivedDropCreator;
import com.surelogic.dropsea.ir.drops.InRegionPromiseDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "InRegion" promise annotations defining a region.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 */
public final class SimpleUniqueInRegionPromiseDrop extends PromiseDrop<UniqueInRegionNode> implements
    IDerivedDropCreator<InRegionPromiseDrop>, RegionAggregationDrop, IUniquePromise {

  public SimpleUniqueInRegionPromiseDrop(UniqueInRegionNode n) {
    super(n);
    setCategorizingMessage(JavaGlobals.REGION_CAT);
  }

  /**
   * Region definitions are not checked by analysis (other than the promise
   * scrubber).
   */
  @Override
  public boolean isIntendedToBeCheckedByAnalysis() {
    return true;
  }

  @Override
  public void validated(final InRegionPromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }

  @Override
  public SimpleUniqueInRegionPromiseDrop getDrop() {
    return this;
  }

  @Override
  public Map<IRegion, IRegion> getAggregationMap(final IRNode fieldDecl) {
    final RegionModel instanceRegion = RegionModel.getInstanceRegion(fieldDecl);
    final IRegion dest = this.getAAST().getSpec().resolveBinding().getRegion();
    return Collections.<IRegion, IRegion> singletonMap(instanceRegion, dest);
  }
}