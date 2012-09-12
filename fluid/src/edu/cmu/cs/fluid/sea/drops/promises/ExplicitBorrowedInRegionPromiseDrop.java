package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.surelogic.aast.promise.ExplicitBorrowedInRegionNode;
import com.surelogic.aast.promise.RegionMappingNode;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * Promise drop for "aggregate" promise annotations defining a region.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 */
public final class ExplicitBorrowedInRegionPromiseDrop extends PromiseDrop<ExplicitBorrowedInRegionNode>
implements RegionAggregationDrop {
  public ExplicitBorrowedInRegionPromiseDrop(ExplicitBorrowedInRegionNode n) {
    super(n);
    setCategory(JavaGlobals.REGION_CAT);
  }
  
  @Override
  public boolean isIntendedToBeCheckedByAnalysis() {
    return true;
  }
  
  @Override
  protected void computeBasedOnAST() {    
    if (getAAST() != null) {
      final String name = JavaNames.getFieldDecl(getNode());
      final String mappings = getAAST().getMapping().unparse(false);
      setResultMessage(
          Messages.RegionAnnotation_borrowedInRegionDrop, mappings, name); 
    }
//    for (RegionMappingNode m : getAST().getMapping().getMappingList()) {
//      IRegionBinding b = m.getTo().resolveBinding();
//      b.getModel().addDependent(this);
//    }
  }
  
  public Map<IRegion, IRegion> getAggregationMap(final IRNode fieldDecl) {
    final Map<IRegion, IRegion> aggregationMap = new HashMap<IRegion, IRegion>();
    for (final RegionMappingNode mapping :
        this.getAAST().getMapping().getMappingList()) {
      aggregationMap.put(mapping.getFrom().resolveBinding().getModel(), 
                         mapping.getTo().resolveBinding().getRegion());
    }
    return Collections.unmodifiableMap(aggregationMap);
  }
}