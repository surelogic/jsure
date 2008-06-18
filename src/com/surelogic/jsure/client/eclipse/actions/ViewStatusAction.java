package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class ViewStatusAction extends Action {
  @Override
  public void run() {
    try {
      PlatformUI.getWorkbench() .getActiveWorkbenchWindow() .getActivePage() .showView("edu.cmu.cs.fluid.dcf.views.coe.ResultsView");
    } catch (PartInitException e) {
      notifyResult(false);
      e.printStackTrace();
    }
  }
}
