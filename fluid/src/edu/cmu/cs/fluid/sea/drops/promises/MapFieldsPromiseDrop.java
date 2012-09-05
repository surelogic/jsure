package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * Promise drop for "InRegion" promise annotations defining a region.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.Region
 * @see edu.cmu.cs.fluid.java.bind.RegionAnnotation
 */
public final class MapFieldsPromiseDrop extends PromiseDrop<FieldMappingsNode> 
implements IDerivedDropCreator<InRegionPromiseDrop>
{
  public MapFieldsPromiseDrop(FieldMappingsNode a) {
    super(a);
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
    if (getAAST() == null) {
      return;
    }
    StringBuffer fieldNames = new StringBuffer();
    boolean first = true;
    for(RegionSpecificationNode reg : getAAST().getFieldsList()) {
      if (first) {
        first = false;
      } else {
        fieldNames.append(", ");
      }
      fieldNames.append(reg);
    }
    String regionName = getAAST().getTo().toString();
    setResultMessage(Messages.RegionAnnotation_mapFieldsDrop, fieldNames,
               regionName);
  }
  
  public void validated(InRegionPromiseDrop pd) {
	pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}