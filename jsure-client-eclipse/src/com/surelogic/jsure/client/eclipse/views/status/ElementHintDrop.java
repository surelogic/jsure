package com.surelogic.jsure.client.eclipse.views.status;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IHintDrop;

final class ElementHintDrop extends ElementDrop {

  protected ElementHintDrop(Element parent, @NonNull IHintDrop hintDrop) {
    super(parent, hintDrop);
  }

  @Override
  @NonNull
  IHintDrop getDrop() {
    return (IHintDrop) super.getDrop();
  }

  @Override
  @NonNull
  Element[] constructChildren() {
    if (getAncestorWithSameDropOrNull() != null)
      return EMPTY;

    final ElementCategory.Categorizer c = new ElementCategory.Categorizer(this);
    c.addAll(getDrop().getProposals());
    c.addAll(getDrop().getHints());
    if (c.isEmpty())
      return EMPTY;
    else
      return c.getAllElementsAsArray();
  }
}
