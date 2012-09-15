package com.surelogic.dropsea.ir.drops.regions;

import com.surelogic.aast.promise.InRegionNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

/**
 * Promise drop for "InRegion" promise annotations defining a region.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 */
public final class InRegionPromiseDrop extends PromiseDrop<InRegionNode> {

  public InRegionPromiseDrop(InRegionNode n) {
    super(n);
    String name = JavaNames.getFieldDecl(getNode());
    String regionName = getAAST().getSpec().unparse(false);
    setMessage(Messages.RegionAnnotation_inRegionDrop, regionName, name); //$NON-NLS-1$
  }

  /**
   * Region definitions are not checked by analysis (other than the promise
   * scrubber).
   */
  @Override
  public boolean isIntendedToBeCheckedByAnalysis() {
    return false;
  }
}