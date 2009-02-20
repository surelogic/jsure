package edu.cmu.cs.fluid.dcf.views.coe;

import org.eclipse.jface.viewers.*;

public interface IResultsViewContentProvider extends ITreeContentProvider {
  IResultsViewContentProvider buildModelOfDropSea();
  
  boolean isShowInferences();
  void setShowInferences(boolean toggle);
}
