package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.aast.promise.AggregateNode;
import com.surelogic.aast.promise.RegionMappingNode;

import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * Promise drop for "aggregate" promise annotations defining a region.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 * @see edu.cmu.cs.fluid.java.bind.RegionAnnotation
 */
public final class AggregatePromiseDrop extends PromiseDrop<AggregateNode> {
  public AggregatePromiseDrop(AggregateNode n) {
    super(n);
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
      setMessage(Messages.RegionAnnotation_aggregateDrop+' '+getAST().getSpec().unparse(false));
      for(RegionMappingNode m : getAST().getSpec().getMappingList()) {
        IRegionBinding b = m.getTo().resolveBinding();
        b.getModel().addDependent(this);
      }
    }
  }
}