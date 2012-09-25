package com.surelogic.jsure.client.eclipse.views.verification;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.surelogic.NonNull;
import com.surelogic.Utility;
import com.surelogic.common.CommonImages;
import com.surelogic.jsure.client.eclipse.views.ResultsImageDescriptor;

@Utility
public final class ColumnLabelProviderUtility {

  static ColumnLabelProvider TREE = new AbstractElementColumnLabelProvider() {

    @Override
    Image getImageFromElement(@NonNull Element element) {
      return element.getImage();
    }

    @Override
    String getTextFromElement(@NonNull Element element) {
      return element.getLabel();
    }
  };

  static ColumnLabelProvider PROJECT = new AbstractElementColumnLabelProvider() {

    private final ResultsImageDescriptor f_projectRid = new ResultsImageDescriptor(CommonImages.IMG_PROJECT, 0,
        VerificationStatusView.ICONSIZE);

    @Override
    Image getImageFromElement(@NonNull Element element) {
      if (isNotEmptyOrNull(getTextFromElement(element)))
        return f_projectRid.getCachedImage();
      else
        return null;
    }

    @Override
    String getTextFromElement(@NonNull Element element) {
      return element.getProjectOrNull();
    }
  };

  static ColumnLabelProvider PACKAGE = new AbstractElementColumnLabelProvider() {

    private final ResultsImageDescriptor f_packageRid = new ResultsImageDescriptor(CommonImages.IMG_PACKAGE, 0,
        VerificationStatusView.ICONSIZE);

    @Override
    Image getImageFromElement(@NonNull Element element) {
      if (isNotEmptyOrNull(getTextFromElement(element)))
        return f_packageRid.getCachedImage();
      else
        return null;
    }

    @Override
    String getTextFromElement(@NonNull Element element) {
      return element.getPackageOrNull();
    }
  };

  static ColumnLabelProvider TYPE = new AbstractElementColumnLabelProvider() {

    private final ResultsImageDescriptor f_classRid = new ResultsImageDescriptor(CommonImages.IMG_CLASS, 0,
        VerificationStatusView.ICONSIZE);

    @Override
    Image getImageFromElement(@NonNull Element element) {
      if (isNotEmptyOrNull(getTextFromElement(element)))
        return f_classRid.getCachedImage();
      else
        return null;
    }

    @Override
    String getTextFromElement(@NonNull Element element) {
      return element.getTypeOrNull();
    }
  };

  static ColumnLabelProvider LINE = new AbstractElementColumnLabelProvider() {

    @Override
    Image getImageFromElement(@NonNull Element element) {
      return null;
    }

    @Override
    String getTextFromElement(@NonNull Element element) {
      return element.getLineNumberAsStringOrNull();
    }
  };

  private ColumnLabelProviderUtility() {
    // no instances
  }
}
