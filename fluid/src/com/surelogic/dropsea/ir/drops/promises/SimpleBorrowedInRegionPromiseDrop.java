package com.surelogic.dropsea.ir.drops.promises;

import java.util.Collections;
import java.util.Map;

import com.surelogic.aast.promise.SimpleBorrowedInRegionNode;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

/**
 * Promise drop for "InRegion" promise annotations defining a region.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 */
public final class SimpleBorrowedInRegionPromiseDrop extends PromiseDrop<SimpleBorrowedInRegionNode> implements
    IDerivedDropCreator<InRegionPromiseDrop>, RegionAggregationDrop {

  public SimpleBorrowedInRegionPromiseDrop(SimpleBorrowedInRegionNode n) {
    super(n);
    setCategory(JavaGlobals.REGION_CAT);
    final String name = JavaNames.getFieldDecl(getNode());
    final String regionName = getAAST().getSpec().unparse(false);
    setMessage(Messages.RegionAnnotation_borrowedInRegionDrop, regionName, name);
  }

  /**
   * Region definitions are not checked by analysis (other than the promise
   * scrubber).
   */
  @Override
  public boolean isIntendedToBeCheckedByAnalysis() {
    return true;
  }

  public void validated(final InRegionPromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }

  public Map<IRegion, IRegion> getAggregationMap(final IRNode fieldDecl) {
    final RegionModel instanceRegion = RegionModel.getInstanceRegion(fieldDecl);
    final IRegion dest = this.getAAST().getSpec().resolveBinding().getRegion();
    return Collections.<IRegion, IRegion> singletonMap(instanceRegion, dest);
  }
}