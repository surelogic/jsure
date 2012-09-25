package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.IResultFolderDrop;

abstract class ElementProofDrop extends ElementDrop {

  static ElementProofDrop factory(Element parent, IProofDrop drop) {
    if (drop instanceof IPromiseDrop)
      return new ElementPromiseDrop(parent, (IPromiseDrop) drop);
    else if (drop instanceof IResultDrop)
      return new ElementResultDrop(parent, (IResultDrop) drop);
    else if (drop instanceof IResultFolderDrop)
      return new ElementResultFolderDrop(parent, (IResultFolderDrop) drop);
    else
      throw new IllegalStateException("Unknown IProofDrop: " + drop);
  }

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
