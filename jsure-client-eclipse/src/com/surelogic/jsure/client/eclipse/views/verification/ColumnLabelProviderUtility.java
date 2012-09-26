package com.surelogic.jsure.client.eclipse.views.verification;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.surelogic.NonNull;
import com.surelogic.Utility;
import com.surelogic.common.CommonImages;
import com.surelogic.jsure.client.eclipse.views.ResultsImageDescriptor;

@Utility
public final class ColumnLabelProviderUtility {

  static final StyledCellLabelProvider TREE = new StyledCellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      if (cell.getElement() instanceof Element) {
        Element element = (Element) cell.getElement();
        final boolean duplicate = hasAncestorWithSameDrop(element);
        final String label;
        if (duplicate) {
          label = "\u2191  " + element.getLabel();
        } else {
          label = element.getLabel();
        }
        cell.setText(label);
        cell.setImage(element.getImage());

        if (element instanceof ElementPromiseDrop) {
          int index = label.indexOf(" on ");
          if (index != -1) {
            StyleRange[] ranges = { new StyleRange(index, label.length() - index, Display.getDefault().getSystemColor(
                SWT.COLOR_DARK_GRAY), null) };
            cell.setStyleRanges(ranges);
          }
        }
      } else
        super.update(cell);
    }

    private boolean hasAncestorWithSameDrop(Element element) {
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
