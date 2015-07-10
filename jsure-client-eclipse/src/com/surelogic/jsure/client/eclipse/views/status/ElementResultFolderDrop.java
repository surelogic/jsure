package com.surelogic.jsure.client.eclipse.views.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.surelogic.NonNull;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IResultFolderDrop;

final class ElementResultFolderDrop extends ElementAnalysisResultDrop {

  static Collection<Element> getInstanceOrElideFolder(Element parent, IResultFolderDrop resultFolderDrop) {
    final List<Element> result = new ArrayList<>();
    if (resultFolderDrop != null)
      if (resultFolderDrop.getLogicOperator() == IResultFolderDrop.LogicOperator.OR && resultFolderDrop.getTrusted().size() == 1) {
        /*
         * Only one OR folder child so don't show the folder to the tool user.
         */
        final ElementCategory.Categorizer c = new ElementCategory.Categorizer(parent);
        c.addAll(resultFolderDrop.getProposals());
        c.addAll(resultFolderDrop.getHints());
        c.addAll(resultFolderDrop.getTrusted());
        result.addAll(c.getAllElements());
      } else
        result.add(new ElementResultFolderDrop(parent, resultFolderDrop));
    return result;
  }

  private ElementResultFolderDrop(Element parent, @NonNull IResultFolderDrop resultFolderDrop) {
    super(parent, resultFolderDrop);
  }

  @Override
  @NonNull
  IResultFolderDrop getDrop() {
    return (IResultFolderDrop) super.getDrop();
  }

  @Override
  String getLabel() {
    return I18N.toStringForUIFolderLabel(super.getLabel(), getChildren().length);
  }
}
