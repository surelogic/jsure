package com.surelogic.jsure.client.eclipse.views.verification;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IDrop;
import com.surelogic.jsure.client.eclipse.views.ResultsImageDescriptor;

import edu.cmu.cs.fluid.java.ISrcRef;

public class XResultsViewLabelProvider implements ITableLabelProvider {

  private boolean m_showInferences = true;

  /**
   * @param showInferences
   *          The showInferences to set.
   */
  public final void setShowInferences(final boolean showInferences) {
    m_showInferences = showInferences;
  }

  public Image getColumnImage(Object element, int columnIndex) {
    switch (columnIndex) {
    case 0: {
      final int flags;
      final String imageName;
      if (element instanceof IDrop) {
        final IDrop drop = (IDrop) element;
        flags = ResultsImageDescriptor.getFlagsFor(drop);
        imageName = ResultsImageDescriptor.getImageNameFor(drop);
      } else if (element instanceof ResultsViewCategoryContent) {
        final ResultsViewCategoryContent content = (ResultsViewCategoryContent) element;
        flags = content.getImageFlags();
        imageName = content.getImageName();
      } else {
        return SLImages.getImage(CommonImages.IMG_UNKNOWN);
      }
      final ResultsImageDescriptor rid = new ResultsImageDescriptor(imageName, flags, VerificationStatusView.ICONSIZE);
      return rid.getCachedImage();
    }
    case 1: {
      if ("".equals(getColumnText(element, columnIndex)))
        return null;
      final ResultsImageDescriptor rid = new ResultsImageDescriptor(CommonImages.IMG_PROJECT, 0, VerificationStatusView.ICONSIZE);
      return rid.getCachedImage();
    }
    case 2: {
      if ("".equals(getColumnText(element, columnIndex)))
        return null;
      final ResultsImageDescriptor rid = new ResultsImageDescriptor(CommonImages.IMG_PACKAGE, 0, VerificationStatusView.ICONSIZE);
      return rid.getCachedImage();
    }
    case 3: {
      if ("".equals(getColumnText(element, columnIndex)))
        return null;
      ResultsImageDescriptor rid = new ResultsImageDescriptor(CommonImages.IMG_CLASS, 0, VerificationStatusView.ICONSIZE);
      return rid.getCachedImage();
    }
    }
    return null;
  }

  public String getColumnText(Object element, int columnIndex) {
    if (element instanceof IDrop) {
      final IDrop drop = (IDrop) element;
      final ISrcRef sr = drop.getSrcRef();
      switch (columnIndex) {
      case 0:
        return drop.getMessage();
      case 1:
        if (sr == null)
          return "";
        else
          return sr.getProject();
      case 2:
        if (sr == null)
          return "";
        else
          return sr.getPackage();
      case 3:
        if (sr == null)
          return "";
        else
          return sr.getCUName();
      case 4:
        if (sr == null)
          return "";
        else {
          final int line = sr.getLineNumber();
          if (line < 1)
            return "";
          else
            return Integer.toString(line);
        }
      }
    }
    if (element instanceof ResultsViewCategoryContent) {
      if (columnIndex == 0) {
        return ((ResultsViewCategoryContent) element).getLabel();
      }
    }
    return "";
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
}
