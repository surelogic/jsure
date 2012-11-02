package com.surelogic.jsure.client.eclipse.views.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IResultFolderDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;

final class ElementResultFolderDrop extends ElementAnalysisResultDrop {

  static Collection<Element> getInstanceOrElideFolder(Element parent, IResultFolderDrop resultFolderDrop) {
    final List<Element> result = new ArrayList<Element>();
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

  private ElementResultFolderDrop(Element parent, IResultFolderDrop resultFolderDrop) {
    super(parent);
    if (resultFolderDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "resultFolderDrop"));
    f_resultFolderDrop = resultFolderDrop;
    final ScanDifferences diff = f_diff;
    if (diff == null) {
      f_diffDrop = null;
    } else {
      if (diff.isNotInOldScan(resultFolderDrop)) {
        f_diffDrop = resultFolderDrop;
      } else {
        f_diffDrop = diff.getChangedInOldScan(resultFolderDrop);
      }
    }
  }

  @NonNull
  private final IResultFolderDrop f_resultFolderDrop;
  /**
   * There are three cases:
   * <ul>
   * <li>if <tt>f_diffDrop == null</tt> the drop is unchanged.</li>
   * <li>if <tt>f_diffDrop == f_resultFolderDrop</tt> the drop is new in this
   * scan.</li>
   * <li>if <tt>f_diffDrop != null && f_diffDrop != f_resultFolderDrop</tt> the
   * drop changed&mdash;and the value of <tt>f_diffDrop</tt> is the old drop.</li>
   * </ul>
   */
  @Nullable
  private IResultFolderDrop f_diffDrop;

  @Override
  @NonNull
  IResultFolderDrop getDrop() {
    return f_resultFolderDrop;
  }

  @Override
  boolean isSame() {
    return f_diffDrop == null;
  }

  @Override
  boolean isNew() {
    return f_diffDrop == f_resultFolderDrop;
  }

  @Override
  @Nullable
  IResultFolderDrop getChangedFromDropOrNull() {
    if (isNew())
      return null;
    else
      return f_diffDrop;
  }

  @Override
  @Nullable
  Image getElementImageChangedFromDropOrNull() {
    final IResultFolderDrop drop = getChangedFromDropOrNull();
    if (drop == null)
      return null;
    else
      return JSureDecoratedImageUtility.getImage(CommonImages.IMG_FOLDER, getImageFlagsHelper(drop));
  }

  @Override
  String getLabel() {
    return I18N.toStringForUIFolderLabel(super.getLabel(), getChildren().length);
  }

  @Override
  @Nullable
  Image getElementImage() {
    return JSureDecoratedImageUtility.getImage(CommonImages.IMG_FOLDER, getImageFlagsHelper(getDrop()));
  }
}
