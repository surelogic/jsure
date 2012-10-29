package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.dropsea.IProofDrop;

abstract class ElementProofDrop extends ElementDrop {

  protected ElementProofDrop(Element parent) {
    super(parent);
  }

  @Override
  abstract IProofDrop getDrop();

  @Override
  @Nullable
  abstract IProofDrop getChangedFromDropOrNull();

  @Override
  int getImageFlagsForChangedFromDrop() {
    final IProofDrop proofDrop = getChangedFromDropOrNull();
    if (proofDrop == null)
      return 0;
    else
      return getImageFlagsHelper(proofDrop);
  }

  @Override
  @Nullable
  String getMessageAboutWhatChangedOrNull() {
    final IProofDrop newDrop = getDrop();
    final IProofDrop oldDrop = getChangedFromDropOrNull();
    if (newDrop == null || oldDrop == null || newDrop == oldDrop)
      return null;
    final StringBuilder b = new StringBuilder();
    if (newDrop.provedConsistent() && !oldDrop.provedConsistent())
      b.append("consistent with code, ");
    else if (!newDrop.provedConsistent() && oldDrop.provedConsistent())
      b.append("not consistent with code, ");
    if (newDrop.proofUsesRedDot() && !oldDrop.proofUsesRedDot())
      b.append("contingent (red-dot), ");
    else if (!newDrop.proofUsesRedDot() && oldDrop.proofUsesRedDot())
      b.append("not contingent (no red-dot), ");
    if (b.length() == 0)
      return null;
    else {
      // remove last ", "
      return b.delete(b.length() - 2, b.length()).toString();
    }
  }

  @Override
  int getImageFlags() {
    final IProofDrop proofDrop = getDrop();
    return getImageFlagsHelper(proofDrop);
  }

  private int getImageFlagsHelper(@NonNull final IProofDrop proofDrop) {
    int flags = 0;
    flags |= proofDrop.provedConsistent() ? CoE_Constants.CONSISTENT : CoE_Constants.INCONSISTENT;
    if (proofDrop.proofUsesRedDot())
      flags |= CoE_Constants.REDDOT;
    return flags;
  }
}
