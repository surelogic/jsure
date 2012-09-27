package com.surelogic.dropsea.ir.drops.uniqueness;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.surelogic.aast.promise.ExplicitBorrowedInRegionNode;
import com.surelogic.aast.promise.RegionMappingNode;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

/**
 * Promise drop for "aggregate" promise annotations defining a region.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 */
public final class ExplicitBorrowedInRegionPromiseDrop extends PromiseDrop<ExplicitBorrowedInRegionNode> implements
    RegionAggregationDrop {

  public ExplicitBorrowedInRegionPromiseDrop(ExplicitBorrowedInRegionNode n) {
    super(n);
    setCategorizingString(JavaGlobals.REGION_CAT);
    final String name = JavaNames.getFieldDecl(getNode());
    final String mappings = getAAST().getMapping().unparse(false);
    setMessage(Messages.RegionAnnotation_borrowedInRegionDrop, mappings, name);
  }

  @Override
  public boolean isIntendedToBeCheckedByAnalysis() {
    return true;
  }

  public Map<IRegion, IRegion> getAggregationMap(final IRNode fieldDecl) {
    final Map<IRegion, IRegion> aggregationMap = new HashMap<IRegion, IRegion>();
    for (final RegionMappingNode mapping : this.getAAST().getMapping().getMappingList()) {
      aggregationMap.put(mapping.getFrom().resolveBinding().getModel(), mapping.getTo().resolveBinding().getRegion());
    }
    return Collections.unmodifiableMap(aggregationMap);
  }
}