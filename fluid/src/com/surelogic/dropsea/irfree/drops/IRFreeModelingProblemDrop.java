package com.surelogic.dropsea.irfree.drops;

import com.surelogic.dropsea.IModelingProblemDrop;
import com.surelogic.dropsea.irfree.JSureEntity;

public class IRFreeModelingProblemDrop extends IRFreeDrop implements IModelingProblemDrop {

  IRFreeModelingProblemDrop(JSureEntity e, Class<?> irClass) {
    super(e, irClass);
  }

  @Override
  boolean aliasTheMessage() {
    return true;
  }
}
