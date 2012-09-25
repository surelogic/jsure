package com.surelogic.jsure.client.eclipse.views.verification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.surelogic.NonNull;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IResultFolderDrop;

final class ElementResultFolderDrop extends ElementAnalysisResultDrop {

  static Collection<Element> getInstanceOrElide(Element parent, IResultFolderDrop resultFolderDrop) {
    final List<Element> result = new ArrayList<Element>();
    if (resultFolderDrop != null)
      if (resultFolderDrop.getLogicOperator() == IResultFolderDrop.LogicOperator.OR && resultFolderDrop.getTrusted().size() == 1) {
        /*
         * Only one OR choice so don't show the folder to the tool user
         */
      } else
        result.add(new ElementResultFolderDrop(parent, resultFolderDrop));
    return result;
  }

  private ElementResultFolderDrop(Element parent, IResultFolderDrop resultFolderDrop) {
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
}
