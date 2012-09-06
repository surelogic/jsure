package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.Collections;
import java.util.Map;

import com.surelogic.aast.promise.*;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * Promise drop for "InRegion" promise annotations defining a region.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 */
public final class SimpleBorrowedInRegionPromiseDrop extends PromiseDrop<SimpleBorrowedInRegionNode> 
implements IDerivedDropCreator<InRegionPromiseDrop>, RegionAggregationDrop {
  public SimpleBorrowedInRegionPromiseDrop(SimpleBorrowedInRegionNode n) {
    super(n);
    setCategory(JavaGlobals.REGION_CAT);
  }
  
  /**
   * Region definitions are not checked by analysis (other than the promise
   * scrubber).
   */
  @Override
  public boolean isIntendedToBeCheckedByAnalysis() {
    return true;
  }

  /**
   * Need to clean up region models.
   * 
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction(Drop)
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    super.deponentInvalidAction(invalidDeponent);
    RegionModel.purgeUnusedRegions();
  }
  
  @Override
  protected void computeBasedOnAST() {
    if (getAAST() != null) {
      final String name       = JavaNames.getFieldDecl(getNode());
      final String regionName = getAAST().getSpec().unparse(false);
      setResultMessage(
          Messages.RegionAnnotation_borrowedInRegionDrop, regionName, name);
    }
  }
  
  public void validated(final InRegionPromiseDrop pd) {
	  pd.setVirtual(true);
	  pd.setSourceDrop(this);
  }
  
  public Map<IRegion, IRegion> getAggregationMap(final IRNode fieldDecl) {
    final RegionModel instanceRegion = RegionModel.getInstanceRegion(fieldDecl);
    final IRegion dest = this.getAAST().getSpec().resolveBinding().getRegion();
    return Collections.<IRegion, IRegion>singletonMap(instanceRegion, dest);
  }
}