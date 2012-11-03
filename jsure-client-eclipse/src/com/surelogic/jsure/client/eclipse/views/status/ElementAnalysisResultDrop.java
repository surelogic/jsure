package com.surelogic.jsure.client.eclipse.views.status;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IAnalysisResultDrop;

abstract class ElementAnalysisResultDrop extends ElementProofDrop {

  protected ElementAnalysisResultDrop(Element parent, @NonNull IAnalysisResultDrop analysisResultDrop) {
    super(parent, analysisResultDrop);
  }

  @Override
  @NonNull
  IAnalysisResultDrop getDrop() {
    return (IAnalysisResultDrop) super.getDrop();
  }

  @Override
  @NonNull
  final Element[] constructChildren() {
    if (getAncestorWithSameDropOrNull() != null)
      return EMPTY;

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
