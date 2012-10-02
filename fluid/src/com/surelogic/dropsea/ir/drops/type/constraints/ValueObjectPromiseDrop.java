package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.*;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.annotation.rules.*;
import com.surelogic.common.XUtil;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.*;

public final class ValueObjectPromiseDrop extends BooleanPromiseDrop<ValueObjectNode> {

  public ValueObjectPromiseDrop(ValueObjectNode a) {
    super(a);
    setCategorizingMessage(Messages.DSC_LAYERS_ISSUES);
  }
}
