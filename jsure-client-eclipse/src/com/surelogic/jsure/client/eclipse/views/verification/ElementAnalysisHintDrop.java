package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IAnalysisHintDrop;

final class ElementAnalysisHintDrop extends ElementDrop {

  protected ElementAnalysisHintDrop(Element parent, IAnalysisHintDrop hintDrop) {
    super(parent);
    if (hintDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "hintDrop"));
    f_resultDrop = hintDrop;
  }

  private final IAnalysisHintDrop f_resultDrop;

  @Override
  @NonNull
  IAnalysisHintDrop getDrop() {
    return f_resultDrop;
  }

  @Override
  int getImageFlags() {
    return 0;
  }

  @Override
  @Nullable
  String getImageName() {
    if (getDrop().getHintType() == IAnalysisHintDrop.HintType.WARNING)
      return CommonImages.IMG_WARNING;
    else
      return CommonImages.IMG_INFO;
  }

  @Override
  @NonNull
  Element[] constructChildren() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int compareTo(Element o) {
    // TODO Auto-generated method stub
    return 0;
  }
}
