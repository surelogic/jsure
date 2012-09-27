package com.surelogic.dropsea.ir.drops;

import com.surelogic.aast.promise.*;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.bind.Messages;

/**
 * Promise drop for "InRegion" promise annotations defining a region.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 */
public final class MapFieldsPromiseDrop extends PromiseDrop<FieldMappingsNode> implements IDerivedDropCreator<InRegionPromiseDrop> {

  public MapFieldsPromiseDrop(FieldMappingsNode a) {
    super(a);
    setCategorizingString(JavaGlobals.REGION_CAT);
    StringBuffer fieldNames = new StringBuffer();
    boolean first = true;
    for (RegionSpecificationNode reg : getAAST().getFieldsList()) {
      if (first) {
        first = false;
      } else {
        fieldNames.append(", ");
      }
      fieldNames.append(reg);
    }
    String regionName = getAAST().getTo().toString();
    setMessage(Messages.RegionAnnotation_mapFieldsDrop, fieldNames, regionName);
  }

  /**
   * Region definitions are not checked by analysis (other than the promise
   * scrubber).
   */
  @Override
  public boolean isIntendedToBeCheckedByAnalysis() {
    return false;
  }

  public void validated(InRegionPromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}