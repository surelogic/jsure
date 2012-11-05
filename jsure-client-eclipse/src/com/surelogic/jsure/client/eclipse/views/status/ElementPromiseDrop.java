package com.surelogic.jsure.client.eclipse.views.status;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IPromiseDrop;

final class ElementPromiseDrop extends ElementProofDrop {

  ElementPromiseDrop(Element parent, @NonNull IPromiseDrop promiseDrop) {
    super(parent, promiseDrop);
  }

  @Override
  @NonNull
  IPromiseDrop getDrop() {
    return (IPromiseDrop) super.getDrop();
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
    c.addAll(getDrop().getCheckedBy());
    c.addAll(getDrop().getDependentPromises());
    if (c.isEmpty())
      return EMPTY;
    else
      return c.getAllElementsAsArray();
  }
}
