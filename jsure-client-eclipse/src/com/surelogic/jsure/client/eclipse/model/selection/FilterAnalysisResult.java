package com.surelogic.jsure.client.eclipse.model.selection;

import org.eclipse.swt.graphics.Image;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.IResultDrop;

public final class FilterAnalysisResult extends Filter implements IOnlyResultsPorus {

  public static final ISelectionFilterFactory FACTORY = new AbstractFilterFactory() {
    public Filter construct(Selection selection, Filter previous) {
      return new FilterAnalysisResult(selection, previous, getFilterLabel());
    }

    public String getFilterLabel() {
      return "Analysis Result";
    }

    @Override
    public Image getFilterImage() {
      return SLImages.getImage(CommonImages.IMG_ANALYSIS_RESULT);
    }
  };

  private FilterAnalysisResult(Selection selection, Filter previous, String filterLabel) {
    super(selection, previous, filterLabel);
  }

  @Override
  public ISelectionFilterFactory getFactory() {
    return FACTORY;
  }

  public static final String CONSISTENT = "Consistent";
  public static final String VOUCHED = "Vouched";
  public static final String INCONSISTENT = "Inconsistent";
  public static final String TIMEOUT = "Timeout";

  @Override
  protected void deriveAllValues() {
    synchronized (this) {
      f_allValues.clear();
      f_allValues.add(CONSISTENT);
      f_allValues.add(VOUCHED);
      f_allValues.add(INCONSISTENT);
      f_allValues.add(TIMEOUT);
    }
  }

  @Override
  public Image getImageFor(String value) {
    if (CONSISTENT.equals(value))
      return SLImages.getImage(CommonImages.IMG_PLUS);
    if (VOUCHED.equals(value))
      return SLImages.getImage(CommonImages.IMG_PLUS_VOUCH);
    if (INCONSISTENT.equals(value))
      return SLImages.getImage(CommonImages.IMG_RED_X);
    if (TIMEOUT.equals(value))
      return SLImages.getImage(CommonImages.IMG_TIMEOUT_X);

    return SLImages.getImage(CommonImages.IMG_EMPTY);
  }

  @Override
  @Nullable
  public String getFilterValueFromDropOrNull(IProofDrop drop) {
    if (drop instanceof IResultDrop) {
      IResultDrop rd = (IResultDrop) drop;
      final String value;
      if (rd.isVouched())
        value = VOUCHED;
      else if (rd.isTimeout())
        value = TIMEOUT;
      else if (rd.isConsistent())
        value = CONSISTENT;
      else
        value = INCONSISTENT;
      return value;
    }
    return null;
  }
}
