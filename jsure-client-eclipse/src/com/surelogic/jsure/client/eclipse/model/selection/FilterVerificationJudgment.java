package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.EnumSet;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility.Flag;

public final class FilterVerificationJudgment extends Filter {

  public static final ISelectionFilterFactory FACTORY = new AbstractFilterFactory() {
    public Filter construct(Selection selection, Filter previous) {
      return new FilterVerificationJudgment(selection, previous, getFilterLabel());
    }

    public String getFilterLabel() {
      return "Verification Judgment";
    }

    @Override
    public Image getFilterImage() {
      return SLImages.getImage(CommonImages.IMG_VERIFICATION_RESULT);
    }
  };

  private FilterVerificationJudgment(Selection selection, Filter previous, String filterLabel) {
    super(selection, previous, filterLabel);
  }

  @Override
  public ISelectionFilterFactory getFactory() {
    return FACTORY;
  }

  public static final String CONSISTENT = "Consistent";
  public static final String CONSISTENT_REDDOT = "Consistent (Contingent)";
  public static final String INCONSISTENT_REDDOT = "Inconsistent (Contingent)";
  public static final String INCONSISTENT = "Inconsistent";

  @Override
  protected void deriveAllValues() {
    synchronized (this) {
      f_allValues.clear();
      f_allValues.add(CONSISTENT);
      f_allValues.add(CONSISTENT_REDDOT);
      f_allValues.add(INCONSISTENT_REDDOT);
      f_allValues.add(INCONSISTENT);
    }
  }

  private final Image imageConsistent = JSureDecoratedImageUtility.getImage(CommonImages.IMG_ANNOTATION,
      EnumSet.of(Flag.CONSISTENT));
  private final Image imageConsistentReddot = JSureDecoratedImageUtility.getImage(CommonImages.IMG_ANNOTATION,
      EnumSet.of(Flag.CONSISTENT, Flag.REDDOT));
  private final Image imageInconsistentReddot = JSureDecoratedImageUtility.getImage(CommonImages.IMG_ANNOTATION,
      EnumSet.of(Flag.INCONSISTENT, Flag.REDDOT));
  private final Image imageInconsistent = JSureDecoratedImageUtility.getImage(CommonImages.IMG_ANNOTATION,
      EnumSet.of(Flag.INCONSISTENT));

  @Override
  public Image getImageFor(String value) {
    if (CONSISTENT.equals(value))
      return imageConsistent;
    if (CONSISTENT_REDDOT.equals(value))
      return imageConsistentReddot;
    if (INCONSISTENT_REDDOT.equals(value))
      return imageInconsistentReddot;
    if (INCONSISTENT.equals(value))
      return imageInconsistent;

    return SLImages.getImage(CommonImages.IMG_EMPTY);
  }

  @Override
  protected void refreshCounts(List<IProofDrop> incomingResults) {
    f_counts.clear();
    int runningTotal = 0;
    for (IProofDrop d : incomingResults) {
      final String value = getFilterValueFromDropOrNull(d);
      if (value != null) {
        Integer count = f_counts.get(value);
        if (count == null) {
          f_counts.put(value, 1);
        } else {
          f_counts.put(value, count + 1);
        }
        runningTotal++;
      }
    }
    f_countTotal = runningTotal;
  }

  @Override
  protected void refreshPorousDrops(List<IProofDrop> incomingResults) {
    f_porousDrops.clear();
    for (IProofDrop d : incomingResults) {
      final String value = getFilterValueFromDropOrNull(d);
      if (value != null) {
        if (f_porousValues.contains(value))
          f_porousDrops.add(d);
      }
    }
  }

  @Override
  @Nullable
  public String getFilterValueFromDropOrNull(IProofDrop drop) {
    if (drop instanceof IPromiseDrop) {
      final boolean reddot = drop.proofUsesRedDot();
      final String value;
      if (drop.provedConsistent())
        value = reddot ? CONSISTENT_REDDOT : CONSISTENT;
      else
        value = reddot ? INCONSISTENT_REDDOT : INCONSISTENT;
      return value;
    }
    return null;
  }
}
