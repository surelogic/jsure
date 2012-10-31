package com.surelogic.jsure.client.eclipse.views.status;

import java.util.EnumSet;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility.Flag;

final class ElementPromiseDrop extends ElementProofDrop {

  ElementPromiseDrop(Element parent, IPromiseDrop promiseDrop) {
    super(parent);
    if (promiseDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "promiseDrop"));
    f_promiseDrop = promiseDrop;
    final ScanDifferences diff = f_diff;
    if (diff == null) {
      f_diffDrop = null;
    } else {
      if (diff.isNotInOldScan(promiseDrop)) {
        f_diffDrop = promiseDrop;
      } else {
        f_diffDrop = diff.getChangedInOldScan(promiseDrop);
      }
    }
  }

  @NonNull
  private final IPromiseDrop f_promiseDrop;
  /**
   * There are three cases:
   * <ul>
   * <li>if <tt>f_diffDrop == null</tt> the drop is unchanged.</li>
   * <li>if <tt>f_diffDrop == f_promiseDrop</tt> the drop is new in this scan.</li>
   * <li>if <tt>f_diffDrop != null && f_diffDrop != f_promiseDrop</tt> the drop
   * changed&mdash;and the value of <tt>f_diffDrop</tt> is the old drop.</li>
   * </ul>
   */
  @Nullable
  private IPromiseDrop f_diffDrop;

  @Override
  @NonNull
  IPromiseDrop getDrop() {
    return f_promiseDrop;
  }

  @Override
  boolean isSame() {
    return f_diffDrop == null;
  }

  @Override
  boolean isNew() {
    return f_diffDrop == f_promiseDrop;
  }

  @Override
  @Nullable
  IPromiseDrop getChangedFromDropOrNull() {
    if (isNew())
      return null;
    else
      return f_diffDrop;
  }

  @Override
  EnumSet<Flag> getImageFlagsForChangedFromDrop() {
    final IPromiseDrop drop = getChangedFromDropOrNull();
    EnumSet<Flag> flags = super.getImageFlagsForChangedFromDrop();
    getImageFlagsHelper(drop, flags);
    return flags;
  }

  @Override
  @NonNull
  String getImageNameForChangedFromDrop() {
    return CommonImages.IMG_ANNOTATION;
  }

  @Override
  EnumSet<Flag> getImageFlags() {
    EnumSet<Flag> flags = super.getImageFlags();
    getImageFlagsHelper(f_promiseDrop, flags);
    return flags;
  }

  private void getImageFlagsHelper(IPromiseDrop drop, EnumSet<Flag> flags) {
    if (drop.isVirtual())
      flags.add(Flag.VIRTUAL);
    if (drop.isAssumed())
      flags.add(Flag.ASSUME);
    if (!drop.isCheckedByAnalysis())
      flags.add(Flag.TRUSTED);
  }

  @Override
  @NonNull
  String getImageName() {
    return CommonImages.IMG_ANNOTATION;
  }

  @Override
  @NonNull
  Element[] constructChildren() {
    /*
     * Only show children if we are not encapsulating the same drop as one of
     * our ancestors.
     */
    if (getAncestorWithSameDropOrNull() == null) {
      final ElementCategory.Categorizer c = new ElementCategory.Categorizer(this);
      c.addAll(getDrop().getProposals());
      if (f_showHints)
        c.addAll(getDrop().getHints());
      c.addAll(getDrop().getCheckedBy());
      c.addAll(getDrop().getDependentPromises());
      if (c.isEmpty())
        return EMPTY;
      else
        return c.getAllElementsAsArray();
    } else
      return EMPTY;
  }
}
