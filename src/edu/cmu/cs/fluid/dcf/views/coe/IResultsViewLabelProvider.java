package edu.cmu.cs.fluid.dcf.views.coe;

import org.eclipse.jface.viewers.ILabelProvider;

public interface IResultsViewLabelProvider extends ILabelProvider {
  /**
   * @param showInferences
   *          The showInferences to set.
   */
  public void setShowInferences(boolean showInferences);
}