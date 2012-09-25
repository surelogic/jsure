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
    return EMPTY;
  }
}
