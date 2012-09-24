package com.surelogic.jsure.client.eclipse.views.results;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.views.ResultsImageDescriptor;

import edu.cmu.cs.fluid.java.ISrcRef;

public class ResultsViewLabelProvider implements ITableLabelProvider {

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
    case 0:
      if (element instanceof ResultsViewContent) {
        final ResultsViewContent c = (ResultsViewContent) element;
        int flags = c.getImageFlags();
        if (m_showInferences) {
          if (c.isWarningDecorated()) {
            flags |= CoE_Constants.HINT_WARNING;
          } else if (c.isInfoDecorated()) {
            if (!CommonImages.IMG_INFO.equals(c.getBaseImageName()))
              flags |= CoE_Constants.HINT_INFO;
          }
        }
        ImageDescriptor id = SLImages.getImageDescriptor(c.getBaseImageName());
        ResultsImageDescriptor rid = new ResultsImageDescriptor(id, flags, ResultsView.ICONSIZE);
        return rid.getCachedImage();
      }
      return SLImages.getImage(CommonImages.IMG_UNKNOWN);
    case 1: {
      if ("".equals(getColumnText(element, columnIndex)))
        return null;
      ImageDescriptor id = SLImages.getImageDescriptor(CommonImages.IMG_PROJECT);
      ResultsImageDescriptor rid = new ResultsImageDescriptor(id, 0, ResultsView.ICONSIZE);
      return rid.getCachedImage();
    }
    case 2: {
      if ("".equals(getColumnText(element, columnIndex)))
        return null;
      ImageDescriptor id = SLImages.getImageDescriptor(CommonImages.IMG_PACKAGE);
      ResultsImageDescriptor rid = new ResultsImageDescriptor(id, 0, ResultsView.ICONSIZE);
      return rid.getCachedImage();
    }
    case 3: {
      if ("".equals(getColumnText(element, columnIndex)))
        return null;
      ImageDescriptor id = SLImages.getImageDescriptor(CommonImages.IMG_CLASS);
      ResultsImageDescriptor rid = new ResultsImageDescriptor(id, 0, ResultsView.ICONSIZE);
      return rid.getCachedImage();
    }
    }
    return null;
  }

  public String getColumnText(Object element, int columnIndex) {
    if (element instanceof ResultsViewContent) {
      final ResultsViewContent c = (ResultsViewContent) element;
      final ISrcRef sr = c.getSrcRef();
      switch (columnIndex) {
      case 0:
        return c.getMessage();
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
