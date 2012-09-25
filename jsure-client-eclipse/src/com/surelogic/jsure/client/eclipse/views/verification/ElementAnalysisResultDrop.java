package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.dropsea.IAnalysisResultDrop;

abstract class ElementAnalysisResultDrop extends ElementProofDrop {

  protected ElementAnalysisResultDrop(Element parent) {
    super(parent);
  }

  @Override
  abstract IAnalysisResultDrop getDrop();
}
