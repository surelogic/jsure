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
 * @see edu.cmu.cs.fluid.java.bind.RegionAnnotation
 */
public final class SimpleUniqueInRegionPromiseDrop extends PromiseDrop<UniqueInRegionNode> 
implements IDerivedDropCreator<InRegionPromiseDrop>, RegionAggregationDrop, IUniquePromise {
  public SimpleUniqueInRegionPromiseDrop(UniqueInRegionNode n) {
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
    if (getAST() != null) {
      final String name       = JavaNames.getFieldDecl(getNode());
      final String regionName = getAST().getSpec().unparse(false);
      setResultMessage(
          Messages.RegionAnnotation_uniqueInRegionDrop, regionName, name);
    }
  }
  
  public void validated(final InRegionPromiseDrop pd) {
	  pd.setVirtual(true);
	  pd.setSourceDrop(this);
  }
  
  public boolean allowRead() {
	  return getAST().allowRead();
  }
  
  public SimpleUniqueInRegionPromiseDrop getDrop() {
    return this;
  }
  
  public Map<IRegion, IRegion> getAggregationMap(final IRNode fieldDecl) {
    final RegionModel instanceRegion = RegionModel.getInstanceRegion(fieldDecl);
    final IRegion dest = this.getAST().getSpec().resolveBinding().getRegion();
    return Collections.<IRegion, IRegion>singletonMap(instanceRegion, dest);
  }
}