package com.surelogic.dropsea.irfree.drops;

import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.DropType;
import com.surelogic.dropsea.IModelingProblemDrop;

public class IRFreeModelingProblemDrop extends IRFreeDrop implements IModelingProblemDrop {

  IRFreeModelingProblemDrop(Entity e) {
    super(e);
  }

  public final DropType getDropType() {
	return DropType.MODELING_PROBLEM;
  }
  
  @Override
  boolean aliasTheMessage() {
    return true;
  }
}
