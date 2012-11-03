package com.surelogic.jsure.client.eclipse.views.status;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IResultDrop;

final class ElementResultDrop extends ElementAnalysisResultDrop {

  ElementResultDrop(Element parent, IResultDrop resultDrop) {
    super(parent, resultDrop);
  }

  @Override
  @NonNull
  IResultDrop getDrop() {
    return (IResultDrop) super.getDrop();
  }
}
