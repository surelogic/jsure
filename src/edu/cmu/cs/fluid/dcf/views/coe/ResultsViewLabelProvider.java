package edu.cmu.cs.fluid.dcf.views.coe;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.surelogic.jsure.coe.JSureViewConstants;
import com.surelogic.xml.results.coe.CoE_Constants;

import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;

public final class ResultsViewLabelProvider extends LabelProvider implements
    IResultsViewLabelProvider {
  private boolean m_showInferences = true;

  /**
   * @param showInferences
   *          The showInferences to set.
   */
  public final void setShowInferences(final boolean showInferences) {
    m_showInferences = showInferences;
  }

  @Override
  public String getText(final Object obj) {
    if (obj instanceof Content) {
      Content c = (Content) obj;
      /*
      if (c.referencedDrop != null) {
        return c.referencedDrop.getClass().getSimpleName()+": "+c.getMessage();
      }
      */
      return c.getMessage();
    } 
    return "invalid: not of type Content";
  }

  @Override
  public Image getImage(final Object obj) {
    if (obj instanceof Content) {
      Content c = (Content) obj;
      int flags = c.getImageFlags();
      if (m_showInferences) {
        if (c.isInfoWarningDecorate) {
          flags |= CoE_Constants.INFO_WARNING;
        } else if (c.isInfoDecorated) {
          if (c.getBaseImageName() != JSureViewConstants.INFO_NAME)
            flags |= CoE_Constants.INFO;
        }
      }
      String name                = c.getBaseImageName();
      ImageDescriptor id         = AbstractDoubleCheckerView.getMatchingDescriptor(name); 
      ResultsImageDescriptor rid = new ResultsImageDescriptor(id,
          flags, AbstractDoubleCheckerView.ICONSIZE);
      return rid.getCachedImage();
    } 
    return ResultsView.UNKNOWN_IMG;
  }
}