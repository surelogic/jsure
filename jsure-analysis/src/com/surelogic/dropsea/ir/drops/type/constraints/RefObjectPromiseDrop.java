package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.RefObjectNode;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.annotation.scrub.ValidatedDropCallback;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

public final class RefObjectPromiseDrop extends BooleanPromiseDrop<RefObjectNode> implements
    ValidatedDropCallback<RefObjectPromiseDrop> {
  public RefObjectPromiseDrop(RefObjectNode a) {
    super(a);
    setCategorizingMessage(Messages.DSC_LAYERS_ISSUES);
  }

  @Override
  public void validated(final RefObjectPromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}
