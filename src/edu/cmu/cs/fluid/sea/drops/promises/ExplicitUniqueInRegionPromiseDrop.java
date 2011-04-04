package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.UniqueMappingNode;

import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * Promise drop for "aggregate" promise annotations defining a region.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 * @see edu.cmu.cs.fluid.java.bind.RegionAnnotation
 */
public final class ExplicitUniqueInRegionPromiseDrop extends PromiseDrop<UniqueMappingNode> {
  public ExplicitUniqueInRegionPromiseDrop(UniqueMappingNode n) {
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
  public boolean isIntendedToBeCheckedByAnalysis() {
    return true;
  }
  
  @Override
  protected void computeBasedOnAST() {    
    if (getAST() != null) {
      final String name = JavaNames.getFieldDecl(getNode());
      final String mappings = getAST().getMapping().unparse(false);
      setResultMessage(
          Messages.RegionAnnotation_aggregateInRegionDrop, mappings, name); 
    }
//      for(RegionMappingNode m : getAST().getSpec().getMappingList()) {
//        IRegionBinding b = m.getTo().resolveBinding();
//        b.getModel().addDependent(this);
//      }
  }
}