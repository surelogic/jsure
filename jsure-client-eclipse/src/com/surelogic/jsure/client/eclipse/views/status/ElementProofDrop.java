package com.surelogic.jsure.client.eclipse.views.status;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IProofDrop;

abstract class ElementProofDrop extends ElementDrop {

  protected ElementProofDrop(Element parent, @NonNull IProofDrop proofDrop) {
    super(parent, proofDrop);
  }

  @Override
  @NonNull
  IProofDrop getDrop() {
    return (IProofDrop) super.getDrop();
  }
}
