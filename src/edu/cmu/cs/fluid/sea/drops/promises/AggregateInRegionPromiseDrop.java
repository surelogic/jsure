package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.*;
import com.surelogic.annotation.scrub.ValidatedDropCallback;

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
public final class AggregateInRegionPromiseDrop extends PromiseDrop<AggregateInRegionNode> 
implements ValidatedDropCallback<AggregatePromiseDrop> {
  public AggregateInRegionPromiseDrop(AggregateInRegionNode n) {
    super(n);
    setCategory(JavaGlobals.REGION_CAT);
  }
  
  /**
   * Region definitions are not checked by analysis (other than the promise
   * scrubber).
   */
  @Override
  public boolean isIntendedToBeCheckedByAnalysis() {
    return false;
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
      String name       = JavaNames.getFieldDecl(getNode());
      String regionName = getAST().getSpec().unparse(false);
      setMessage(Messages.RegionAnnotation_aggregateInRegionDrop, regionName, name); //$NON-NLS-1$
    }
  }
  
  public void validated(AggregatePromiseDrop pd) {
	  pd.setVirtual(true);
	  pd.setSourceDrop(this);
  }
}