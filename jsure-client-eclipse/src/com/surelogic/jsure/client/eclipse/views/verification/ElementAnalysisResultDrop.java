package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IAnalysisResultDrop;

abstract class ElementAnalysisResultDrop extends ElementProofDrop {

  protected ElementAnalysisResultDrop(Element parent) {
    super(parent);
  }

  @Override
  abstract IAnalysisResultDrop getDrop();

  @Override
  @NonNull
  final Element[] constructChildren() {
    final ElementCategory.Categorizer c = new ElementCategory.Categorizer(this);
    c.addAll(getDrop().getProposals());
    if (f_showHints)
      c.addAll(getDrop().getHints());
    c.addAll(getDrop().getTrusted());
    if (c.isEmpty())
      return EMPTY;
    else
      return c.getAllElementsAsArray();
  }
}
