package com.surelogic.jsure.client.eclipse.model.selection;

import org.eclipse.swt.graphics.Image;

import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IProofDrop;

public final class FilterJavaPackage extends Filter {

  public static final ISelectionFilterFactory FACTORY = new AbstractFilterFactory() {
    @Override
    public Filter construct(Selection selection, Filter previous) {
      return new FilterJavaPackage(selection, previous, getFilterLabel());
    }

    @Override
    public String getFilterLabel() {
      return "Java Package";
    }

    @Override
    public Image getFilterImage() {
      return SLImages.getImage(CommonImages.IMG_PACKAGE);
    }
  };

  private FilterJavaPackage(Selection selection, Filter previous, String filterLabel) {
    super(selection, previous, filterLabel);
  }

  @Override
  public ISelectionFilterFactory getFactory() {
    return FACTORY;
  }

  @Override
  public Image getImageFor(String value) {
    return SLImages.getImage(CommonImages.IMG_PACKAGE);
  }

  @Override
  @Nullable
  public String getFilterValueFromDropOrNull(IProofDrop drop) {
    final IJavaRef jr = drop.getJavaRef();
    if (jr != null) {
      final String value = jr.getPackageName();
      return value;
    }
    return null;
  }
}
