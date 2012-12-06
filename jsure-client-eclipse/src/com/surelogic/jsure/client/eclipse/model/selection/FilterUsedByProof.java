package com.surelogic.jsure.client.eclipse.model.selection;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IProofDrop;

public final class FilterUsedByProof extends Filter implements IOnlyResultsPorus {

  public static final ISelectionFilterFactory FACTORY = new AbstractFilterFactory() {
    public Filter construct(Selection selection, Filter previous) {
      return new FilterUsedByProof(selection, previous, getFilterLabel());
    }

    public String getFilterLabel() {
      return "Used By Proof";
    }

    @Override
    public Image getFilterImage() {
      return SLImages.getImage(CommonImages.IMG_CHOICE);
    }
  };

  private FilterUsedByProof(Selection selection, Filter previous, String filterLabel) {
    super(selection, previous, filterLabel);
  }

  @Override
  public ISelectionFilterFactory getFactory() {
    return FACTORY;
  }

  public static final String USED = "Used";
  public static final String NOT_USED = "Not Used";

  @Override
  protected void deriveAllValues() {
    synchronized (this) {
      f_allValues.clear();
      f_allValues.add(USED);
      f_allValues.add(NOT_USED);
    }
  }

  private final Image f_unused = SLImages.getDecoratedGrayscaleImage(CommonImages.IMG_CHOICE, new ImageDescriptor[] { null, null,
      SLImages.getImageDescriptor(CommonImages.DECR_UNUSED), null, null });

  @Override
  public Image getImageFor(String value) {
    return USED.equals(value) ? SLImages.getImage(CommonImages.IMG_CHOICE) : f_unused;
  }

  @Override
  @Nullable
  public String getFilterValueFromDropOrNull(IProofDrop drop) {
    if (drop instanceof IAnalysisResultDrop) {
      return ((IAnalysisResultDrop) drop).usedByProof() ? USED : NOT_USED;
    }
    return null;
  }
}
