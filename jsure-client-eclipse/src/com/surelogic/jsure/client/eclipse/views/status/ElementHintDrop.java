package com.surelogic.jsure.client.eclipse.views.status;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;

final class ElementHintDrop extends ElementDrop {

  protected ElementHintDrop(Element parent, IHintDrop hintDrop) {
    super(parent);
    if (hintDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "hintDrop"));
    f_hintDrop = hintDrop;
    final ScanDifferences diff = f_diff;
    if (diff == null) {
      f_diffDrop = null;
    } else {
      if (diff.isNotInOldScan(hintDrop)) {
        f_diffDrop = hintDrop;
      } else {
        f_diffDrop = diff.getChangedInOldScan(hintDrop);
      }
    }
  }

  @NonNull
  private final IHintDrop f_hintDrop;
  /**
   * There are three cases:
   * <ul>
   * <li>if <tt>f_diffDrop == null</tt> the drop is unchanged.</li>
   * <li>if <tt>f_diffDrop == f_hintDrop</tt> the drop is new in this scan.</li>
   * <li>if <tt>f_diffDrop != null && f_diffDrop != f_hintDrop</tt> the drop
   * changed&mdash;and the value of <tt>f_diffDrop</tt> is the old drop.</li>
   * </ul>
   */
  @Nullable
  private IHintDrop f_diffDrop;

  @Override
  @NonNull
  IHintDrop getDrop() {
    return f_hintDrop;
  }

  @Override
  boolean isSame() {
    return f_diffDrop == null;
  }

  @Override
  boolean isNew() {
    return f_diffDrop == f_hintDrop;
  }

  @Override
  @Nullable
  IHintDrop getChangedFromDropOrNull() {
    if (isNew())
      return null;
    else
      return f_diffDrop;
  }

  @Override
  @Nullable
  Image getElementImageChangedFromDropOrNull() {
    final IHintDrop drop = getChangedFromDropOrNull();
    if (drop == null)
      return null;
    else
      return JSureDecoratedImageUtility.getImage(getImageNameHelper(drop));
  }

  @Override
  @NonNull
  String getMessageAboutWhatChangedOrNull() {
    return null;
  }

  @Override
  @Nullable
  Image getElementImage() {
    return JSureDecoratedImageUtility.getImage(getImageNameHelper(getDrop()));
  }

  @Nullable
  private String getImageNameHelper(IHintDrop drop) {
    if (drop == null)
      return null;

    if (drop.getHintType() == IHintDrop.HintType.WARNING)
      return CommonImages.IMG_WARNING;
    else
      return CommonImages.IMG_INFO;
  }

  @Override
  @NonNull
  Element[] constructChildren() {
    if (getAncestorWithSameDropOrNull() != null)
      return EMPTY;

    final ElementCategory.Categorizer c = new ElementCategory.Categorizer(this);
    c.addAll(getDrop().getProposals());
    c.addAll(getDrop().getHints());
    if (c.isEmpty())
      return EMPTY;
    else
      return c.getAllElementsAsArray();
  }
}
