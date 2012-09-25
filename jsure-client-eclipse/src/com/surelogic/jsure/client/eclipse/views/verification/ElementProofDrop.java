package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.dropsea.IProofDrop;

abstract class ElementProofDrop extends ElementDrop {

  protected ElementProofDrop(Element parent) {
    super(parent);
  }

  @Override
  abstract IProofDrop getDrop();

  @Override
  int getImageFlags() {
    IProofDrop proofDrop = getDrop();
    int flags = 0;
    flags |= proofDrop.provedConsistent() ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT;
    if (proofDrop.proofUsesRedDot())
      flags |= CoE_Constants.REDDOT;
    return flags;
  }
}
