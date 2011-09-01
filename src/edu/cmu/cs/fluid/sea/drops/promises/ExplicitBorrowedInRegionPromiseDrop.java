package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.ExplicitBorrowedInRegionNode;

import edu.cmu.cs.fluid.java.JavaGlobals;
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
public final class ExplicitBorrowedInRegionPromiseDrop extends PromiseDrop<ExplicitBorrowedInRegionNode> {
  public ExplicitBorrowedInRegionPromiseDrop(ExplicitBorrowedInRegionNode n) {
    super(n);
    setCategory(JavaGlobals.REGION_CAT);
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
          Messages.RegionAnnotation_borrowedInRegionDrop, mappings, name); 
    }
//    for (RegionMappingNode m : getAST().getMapping().getMappingList()) {
//      IRegionBinding b = m.getTo().resolveBinding();
//      b.getModel().addDependent(this);
//    }
  }
}