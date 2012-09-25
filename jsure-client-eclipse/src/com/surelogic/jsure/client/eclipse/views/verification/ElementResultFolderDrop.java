package com.surelogic.jsure.client.eclipse.views.verification;

import com.surelogic.NonNull;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IResultFolderDrop;

final class ElementResultFolderDrop extends ElementAnalysisResultDrop {

  ElementResultFolderDrop(Element parent, IResultFolderDrop resultFolderDrop) {
    super(parent);
    if (resultFolderDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "resultFolderDrop"));
    f_resultFolderDrop = resultFolderDrop;
  }

  private final IResultFolderDrop f_resultFolderDrop;

  @Override
  @NonNull
  IResultFolderDrop getDrop() {
    return f_resultFolderDrop;
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
    if (getDrop().getLogicOperator() == IResultFolderDrop.LogicOperator.AND)
      return CommonImages.IMG_FOLDER;
    else
      return CommonImages.IMG_FOLDER_OR;
  }

  @Override
  public int compareTo(Element o) {
    // TODO Auto-generated method stub
    return 0;
  }
}
