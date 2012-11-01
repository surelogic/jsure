package com.surelogic.jsure.client.eclipse.views.status;

import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;

final class ElementProposedPromiseDrop extends ElementDrop {

  protected ElementProposedPromiseDrop(Element parent, IProposedPromiseDrop proposedPromiseDrop) {
    super(parent);
    if (proposedPromiseDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "proposedPromiseDrop"));
    f_proposedPromiseDrop = proposedPromiseDrop;
    final ScanDifferences diff = f_diff;
    if (diff == null) {
      f_diffDrop = null;
    } else {
      if (diff.isNotInOldScan(proposedPromiseDrop)) {
        f_diffDrop = proposedPromiseDrop;
      } else {
        f_diffDrop = diff.getChangedInOldScan(proposedPromiseDrop);
      }
    }
  }

  @NonNull
  private final IProposedPromiseDrop f_proposedPromiseDrop;
  /**
   * There are three cases:
   * <ul>
   * <li>if <tt>f_diffDrop == null</tt> the drop is unchanged.</li>
   * <li>if <tt>f_diffDrop == f_proposedPromiseDrop</tt> the drop is new in this
   * scan.</li>
   * <li>if <tt>f_diffDrop != null && f_diffDrop != f_proposedPromiseDrop</tt>
   * the drop changed&mdash;and the value of <tt>f_diffDrop</tt> is the old
   * drop.</li>
   * </ul>
   */
  @Nullable
  private IProposedPromiseDrop f_diffDrop;

  @Override
  @NonNull
  IProposedPromiseDrop getDrop() {
    return f_proposedPromiseDrop;
  }

  @Override
  boolean isSame() {
    return f_diffDrop == null;
  }

  @Override
  boolean isNew() {
    return f_diffDrop == f_proposedPromiseDrop;
  }

  @Override
  @Nullable
  IProposedPromiseDrop getChangedFromDropOrNull() {
    if (isNew())
      return null;
    else
      return f_diffDrop;
  }

  @Override
  @Nullable
  Image getElementImageChangedFromDropOrNull() {
    final IProposedPromiseDrop drop = getChangedFromDropOrNull();
    if (drop == null)
      return null;
    else
      return JSureDecoratedImageUtility.getImage(CommonImages.IMG_ANNOTATION_PROPOSED);
  }

  @Override
  @NonNull
  String getMessageAboutWhatChangedOrNull() {
    return null;
  }

  @Override
  @Nullable
  Image getElementImage() {
    return JSureDecoratedImageUtility.getImage(CommonImages.IMG_ANNOTATION_PROPOSED);
  }

  @Override
  @NonNull
  Element[] constructChildren() {
    final ElementCategory.Categorizer c = new ElementCategory.Categorizer(this);
    c.addAll(getDrop().getProposals());
    if (f_showHints)
      c.addAll(getDrop().getHints());
    if (c.isEmpty())
      return EMPTY;
    else
      return c.getAllElementsAsArray();
  }
}
