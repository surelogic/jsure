package com.surelogic.jsure.client.eclipse.model.selection;

import org.eclipse.swt.graphics.Image;

import com.surelogic.Nullable;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IProofDrop;

public final class FilterProject extends Filter {

  public static final ISelectionFilterFactory FACTORY = new AbstractFilterFactory() {
    @Override
    public Filter construct(Selection selection, Filter previous) {
      return new FilterProject(selection, previous, getFilterLabel());
    }

    @Override
    public String getFilterLabel() {
      return "Project";
    }

    @Override
    public Image getFilterImage() {
      return SLImages.getImageForJavaProject();
    }
  };

  FilterProject(Selection selection, Filter previous, String filterLabel) {
    super(selection, previous, filterLabel);
  }

  @Override
  public ISelectionFilterFactory getFactory() {
    return FACTORY;
  }

  @Override
  public Image getImageFor(String value) {
    return SLImages.getImageForProject(value);
  }

  @Override
  @Nullable
  public String getFilterValueFromDropOrNull(IProofDrop drop) {
    final IJavaRef jr = drop.getJavaRef();
    if (jr != null) {
      String result = jr.getEclipseProjectName();
      if (result.contains("JRE_CONTAINER")) {
        result = "Java Standard Library";
      }
      return result;
    }
    return null;
  }
}
