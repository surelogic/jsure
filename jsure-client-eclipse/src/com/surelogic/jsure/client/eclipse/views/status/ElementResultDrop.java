package com.surelogic.jsure.client.eclipse.views.status;

import java.util.EnumSet;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility.Flag;

final class ElementResultDrop extends ElementAnalysisResultDrop {

  ElementResultDrop(Element parent, IResultDrop resultDrop) {
    super(parent);
    if (resultDrop == null)
      throw new IllegalArgumentException(I18N.err(44, "resultDrop"));
    f_resultDrop = resultDrop;
    final ScanDifferences diff = f_diff;
    if (diff == null) {
      f_diffDrop = null;
    } else {
      if (diff.isNotInOldScan(resultDrop)) {
        f_diffDrop = resultDrop;
      } else {
        f_diffDrop = diff.getChangedInOldScan(resultDrop);
      }
    }
  }

  @NonNull
  private final IResultDrop f_resultDrop;
  /**
   * There are three cases:
   * <ul>
   * <li>if <tt>f_diffDrop == null</tt> the drop is unchanged.</li>
   * <li>if <tt>f_diffDrop == f_resultDrop</tt> the drop is new in this scan.</li>
   * <li>if <tt>f_diffDrop != null && f_diffDrop != f_resultDrop</tt> the drop
   * changed&mdash;and the value of <tt>f_diffDrop</tt> is the old drop.</li>
   * </ul>
   */
  @Nullable
  private IResultDrop f_diffDrop;

  @Override
  @NonNull
  IResultDrop getDrop() {
    return f_resultDrop;
  }

  @Override
  boolean isSame() {
    return f_diffDrop == null;
  }

  @Override
  boolean isNew() {
    return f_diffDrop == f_resultDrop;
  }

  @Override
  @Nullable
  IResultDrop getChangedFromDropOrNull() {
    if (isNew())
      return null;
    else
      return f_diffDrop;
  }

  @Override
  @Nullable
  String getImageNameForChangedFromDrop() {
    return getImageNameHelper(getChangedFromDropOrNull());
  }

  @Override
  EnumSet<Flag> getImageFlags() {
    if (hasChildren())
      return super.getImageFlags();
    else
      return EnumSet.noneOf(Flag.class);
  }

  @Override
  @Nullable
  String getImageName() {
    return getImageNameHelper(getDrop());
  }

  @Nullable
  private String getImageNameHelper(IResultDrop drop) {
    if (drop == null)
      return null;

    if (drop.isConsistent())
      return CommonImages.IMG_PLUS;
    else {
      if (drop.isVouched())
        return CommonImages.IMG_PLUS_VOUCH;
      else
        return CommonImages.IMG_RED_X;
    }
  }

  @Override
  @Nullable
  String getMessageAboutWhatChangedOrNull() {
    final IResultDrop newDrop = getDrop();
    final IResultDrop oldDrop = getChangedFromDropOrNull();
    if (newDrop == null || oldDrop == null || newDrop == oldDrop)
      return null;
    final String superMsg = super.getMessageAboutWhatChangedOrNull();
    StringBuilder b = new StringBuilder(superMsg == null ? "" : superMsg);
    b.append(", ");
    if (newDrop.isConsistent() && !oldDrop.isConsistent())
      b.append("consistent analysis result, ");
    else if (!newDrop.isConsistent() && oldDrop.isConsistent())
      b.append("inconsistent analysis result, ");
    if (newDrop.isTimeout() && !oldDrop.isTimeout())
      b.append("analysis execution timed out, ");
    else if (!newDrop.isTimeout() && oldDrop.isTimeout())
      b.append("analysis execution did not time out, ");
    if (newDrop.isVouched() && !oldDrop.isVouched())
      b.append("programmer vouch, ");
    else if (!newDrop.isVouched() && oldDrop.isVouched())
      b.append("no programmer vouch, ");
    if (b.length() == 0)
      return null;
    else {
      // remove last ", "
      return b.delete(b.length() - 2, b.length()).toString();
    }
  }
}
