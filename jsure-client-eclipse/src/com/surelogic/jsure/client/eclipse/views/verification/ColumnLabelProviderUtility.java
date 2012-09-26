package com.surelogic.jsure.client.eclipse.views.verification;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.surelogic.NonNull;
import com.surelogic.Utility;
import com.surelogic.common.CommonImages;
import com.surelogic.jsure.client.eclipse.views.ResultsImageDescriptor;

@Utility
public final class ColumnLabelProviderUtility {

  static final ColumnLabelProvider TREE = new AbstractElementColumnLabelProvider() {

    private Color f_duplicate;

    @Override
    Image getImageFromElement(@NonNull Element element) {
      return element.getImage();
    }

    @Override
    String getTextFromElement(@NonNull Element element) {
      if (hasAncestorWithSameDrop(element)) {
        return "\u2191  " + element.getLabel();
      }
      return element.getLabel();
    }

    @Override
    public Color getForeground(Object element) {
      if (hasAncestorWithSameDrop(element)) {
        if (f_duplicate == null) {
          f_duplicate = new Color(Display.getCurrent(), 149, 125, 71);
          Display.getCurrent().disposeExec(new Runnable() {
            public void run() {
              f_duplicate.dispose();
            }
          });
        }
        return f_duplicate;
      }
      return super.getForeground(element);
    }

    private boolean hasAncestorWithSameDrop(Object element) {
      if (element instanceof ElementDrop)
        if (((ElementDrop) element).getAncestorWithSameDropOrNull() != null)
          return true;
      return false;
    }
  };

  static final ColumnLabelProvider PROJECT = new AbstractElementColumnLabelProvider() {

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

  static final ColumnLabelProvider PACKAGE = new AbstractElementColumnLabelProvider() {

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

  static final ColumnLabelProvider TYPE = new AbstractElementColumnLabelProvider() {

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
