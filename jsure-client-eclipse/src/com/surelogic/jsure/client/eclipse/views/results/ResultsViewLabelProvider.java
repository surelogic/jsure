package com.surelogic.jsure.client.eclipse.views.results;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.surelogic.common.CommonImages;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.common.ui.SLImages;

public final class ResultsViewLabelProvider extends ColumnLabelProvider {

  private boolean m_showInferences = true;

  /**
   * @param showInferences
   *          The showInferences to set.
   */
  public final void setShowInferences(final boolean showInferences) {
    m_showInferences = showInferences;
  }

  public String getText(final Object obj) {
    if (obj instanceof ResultsViewContent) {
      final ResultsViewContent c = (ResultsViewContent) obj;
      return c.getMessage();
    }
    return "invalid: not of type AbstractContent";
  }

  public Image getImage(final Object obj) {
    if (obj instanceof ResultsViewContent) {
      final ResultsViewContent c = (ResultsViewContent) obj;
      int flags = c.getImageFlags();
      if (m_showInferences) {
        if (c.isWarningDecorated()) {
          flags |= CoE_Constants.INFO_WARNING;
        } else if (c.isInfoDecorated()) {
          if (!CommonImages.IMG_INFO.equals(c.getBaseImageName()))
            flags |= CoE_Constants.INFO;
        }
      }
      ImageDescriptor id = SLImages.getImageDescriptor(c.getBaseImageName());
      ResultsImageDescriptor rid = new ResultsImageDescriptor(id, flags, ResultsView.ICONSIZE);
      return rid.getCachedImage();
    }
    return SLImages.getImage(CommonImages.IMG_UNKNOWN);
  }

  public Image getToolTipImage(Object element) {
    return null;
  }

  public String getToolTipText(Object element) {
    return null;
  }

  public Point getToolTipShift(Object object) {
    return new Point(5, 5);
  }

  public int getToolTipDisplayDelayTime(Object object) {
    return 200;
  }

  public int getToolTipTimeDisplayed(Object object) {
    return 5000;
  }

  public void update(ViewerCell cell) {
    final Object element = cell.getElement();
    cell.setText(getText(element));
    cell.setImage(getImage(element));
  }
}