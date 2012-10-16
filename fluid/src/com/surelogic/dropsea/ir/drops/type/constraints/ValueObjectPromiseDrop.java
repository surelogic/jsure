package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.ValueObjectNode;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.annotation.scrub.ValidatedDropCallback;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

public final class ValueObjectPromiseDrop extends BooleanPromiseDrop<ValueObjectNode> implements
    ValidatedDropCallback<ValueObjectPromiseDrop> {
  public ValueObjectPromiseDrop(ValueObjectNode a) {
    super(a);
    setCategorizingMessage(Messages.DSC_LAYERS_ISSUES);
  }
  
  public void validated(final ValueObjectPromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}
