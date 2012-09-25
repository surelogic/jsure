package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.NonNull;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IResultDrop;

final class ElementResultDrop extends ElementAnalysisResultDrop {

  ElementResultDrop(Element parent, IResultDrop resultDrop) {
    super(parent);
    if (resultDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "resultDrop"));
    f_resultDrop = resultDrop;
  }

  private final IResultDrop f_resultDrop;

  @Override
  @NonNull
  IResultDrop getDrop() {
    return f_resultDrop;
  }

  @Override
  int getImageFlags() {
    if (hasChildren())
      return super.getImageFlags();
    else
      return 0;
  }

  @Override
  String getImageName() {
    if (getDrop().isConsistent())
      return CommonImages.IMG_PLUS;
    else
      return CommonImages.IMG_RED_X;
  }
}
