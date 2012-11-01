package com.surelogic.jsure.client.eclipse.views.status;

import java.util.EnumSet;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility.Flag;

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

  protected EnumSet<Flag> getImageFlagsHelper(@NonNull final IProofDrop proofDrop) {
    EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
    flags.add(proofDrop.provedConsistent() ? Flag.CONSISTENT : Flag.INCONSISTENT);
    if (proofDrop.proofUsesRedDot())
      flags.add(Flag.REDDOT);
    return flags;
  }
}
