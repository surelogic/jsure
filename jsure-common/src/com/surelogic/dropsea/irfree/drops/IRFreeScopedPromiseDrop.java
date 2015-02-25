package com.surelogic.dropsea.irfree.drops;

import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.DropType;
import com.surelogic.dropsea.IScopedPromiseDrop;

public final class IRFreeScopedPromiseDrop extends IRFreePromiseDrop implements IScopedPromiseDrop {

  IRFreeScopedPromiseDrop(Entity e) {
    super(e);
  }
  
  public final DropType getDropType() {
	return DropType.SCOPED_PROMISE;
  }
}
