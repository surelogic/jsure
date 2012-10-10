package com.surelogic.jsure.views.debug.oracleDiff;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class ToggleBaseline implements IViewActionDelegate {
  private IViewPart currentView = null;
  private Shell shell = null;
  
  
  public void init(final IViewPart view) {
    currentView = view;
    shell = view.getViewSite().getShell();
  }

  public void selectionChanged(
      final IAction action, final ISelection selection) {
    // We don't care about selections
  }

  public void run(final IAction action) {
	  if (currentView instanceof SnapshotDiffView) { 
		  SnapshotDiffView v = (SnapshotDiffView) currentView;
		  SnapshotDiffContentProvider p = 
			  (SnapshotDiffContentProvider) v.getViewer().getContentProvider();
		  p.toggleReference();
		  p.build();
		  v.getViewer().refresh();
	  }
  }
}
