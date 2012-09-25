package com.surelogic.jsure.client.eclipse.views.verification;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.jsure.client.eclipse.views.ResultsImageDescriptor;

public class VerificationStatusViewLabelProvider implements ITableLabelProvider {

  private final ResultsImageDescriptor f_projectRid = new ResultsImageDescriptor(CommonImages.IMG_PROJECT, 0,
      VerificationStatusView.ICONSIZE);
  private final ResultsImageDescriptor f_packageRid = new ResultsImageDescriptor(CommonImages.IMG_PACKAGE, 0,
      VerificationStatusView.ICONSIZE);
  private final ResultsImageDescriptor f_classRid = new ResultsImageDescriptor(CommonImages.IMG_CLASS, 0,
      VerificationStatusView.ICONSIZE);

  public Image getColumnImage(Object element, int columnIndex) {
    switch (columnIndex) {
    case 0:
      if (element instanceof Element) {
        return ((Element) element).getImage();
      }
      break;
    case 1:
      if (isNotEmptyOrNull(getColumnText(element, columnIndex)))
        return f_projectRid.getCachedImage();
    case 2:
      if (isNotEmptyOrNull(getColumnText(element, columnIndex)))
        return f_packageRid.getCachedImage();
    case 3:
      if (isNotEmptyOrNull(getColumnText(element, columnIndex)))
        return f_classRid.getCachedImage();
    }
    return null;
  }

  public String getColumnText(Object element, int columnIndex) {
    if (element instanceof Element) {
      final Element el = (Element) element;
      switch (columnIndex) {
      case 0:
        return el.getLabel();
      case 1:
        return el.getPackageOrNull();
      case 2:
        return el.getPackageOrNull();
      case 3:
        return el.getTypeOrNull();
      case 4:
        return el.getLineNumberAsStringOrNull();
      }
    }
    return null;
  }

  public void dispose() {
    // do nothing
  }

  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  public void addListener(ILabelProviderListener listener) {
    // do nothing
  }

  public void removeListener(ILabelProviderListener listener) {
    // do nothing
  }

  private boolean isNotEmptyOrNull(String value) {
    if (value == null)
      return false;
    if ("".equals(value))
      return false;
    return true;
  }
}
