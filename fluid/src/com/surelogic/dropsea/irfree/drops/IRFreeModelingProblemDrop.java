package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.FROM_SRC;

import com.surelogic.dropsea.IModelingProblemDrop;
import com.surelogic.dropsea.irfree.Entity;

public class IRFreeModelingProblemDrop extends IRFreeDrop implements IModelingProblemDrop {

  private final boolean f_isFromSrc;

  IRFreeModelingProblemDrop(Entity e, Class<?> irClass) {
    super(e, irClass);
    f_isFromSrc = "true".equals(e.getAttribute(FROM_SRC));
  }

  @Override
  boolean aliasTheMessage() {
    return true;
  }

  public boolean isFromSrc() {
    return f_isFromSrc;
  }
}
