package com.surelogic.dropsea.ir.drops.method.constraints;

import com.surelogic.aast.promise.MustInvokeOnOverrideNode;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

public final class MustInvokeOnOverridePromiseDrop extends BooleanPromiseDrop<MustInvokeOnOverrideNode> {

  public MustInvokeOnOverridePromiseDrop(MustInvokeOnOverrideNode a) {
    super(a);
    setCategorizingMessage(Messages.DSC_LAYERS_ISSUES);
  }
}
