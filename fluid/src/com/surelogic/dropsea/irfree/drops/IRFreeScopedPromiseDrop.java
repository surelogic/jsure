package com.surelogic.dropsea.irfree.drops;

import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IScopedPromiseDrop;

public final class IRFreeScopedPromiseDrop extends IRFreePromiseDrop implements IScopedPromiseDrop {

  IRFreeScopedPromiseDrop(Entity e, Class<?> irClass) {
    super(e, irClass);
  }
}
