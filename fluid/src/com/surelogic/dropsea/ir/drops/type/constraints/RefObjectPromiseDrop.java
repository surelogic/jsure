package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.*;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

public final class RefObjectPromiseDrop extends BooleanPromiseDrop<RefObjectNode> {

  public RefObjectPromiseDrop(RefObjectNode a) {
    super(a);
    setCategorizingMessage(Messages.DSC_LAYERS_ISSUES);
  }
}
