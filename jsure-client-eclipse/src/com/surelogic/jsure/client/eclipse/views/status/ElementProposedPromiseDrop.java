package com.surelogic.jsure.client.eclipse.views.status;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IProposedPromiseDrop;

final class ElementProposedPromiseDrop extends ElementDrop {

  protected ElementProposedPromiseDrop(Element parent, @NonNull IProposedPromiseDrop proposedPromiseDrop) {
    super(parent, proposedPromiseDrop);
  }

  @Override
  @NonNull
  IProposedPromiseDrop getDrop() {
    return (IProposedPromiseDrop) super.getDrop();
  }

  @Override
  @NonNull
  Element[] constructChildren() {
    if (getAncestorWithSameDropOrNull() != null)
      return EMPTY;

    final ElementCategory.Categorizer c = new ElementCategory.Categorizer(this);
    c.addAll(getDrop().getProposals());
    if (f_showHints)
      c.addAll(getDrop().getHints());
    if (c.isEmpty())
      return EMPTY;
    else
      return c.getAllElementsAsArray();
  }
}
