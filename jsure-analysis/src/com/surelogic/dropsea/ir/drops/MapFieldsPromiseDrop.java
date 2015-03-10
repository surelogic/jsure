package com.surelogic.dropsea.ir.drops;

import com.surelogic.aast.promise.FieldMappingsNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "InRegion" promise annotations defining a region.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 */
public final class MapFieldsPromiseDrop extends PromiseDrop<FieldMappingsNode> implements IDerivedDropCreator<InRegionPromiseDrop> {

  public MapFieldsPromiseDrop(FieldMappingsNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.REGION_CAT);
  }

  /**
   * Region definitions are not checked by analysis (other than the promise
   * scrubber).
   */
  @Override
  public boolean isIntendedToBeCheckedByAnalysis() {
    return false;
  }

  @Override
  public void validated(InRegionPromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}