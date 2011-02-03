package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.ui.SWTUtility;
import com.surelogic.common.ui.dialogs.SendProblemReportDialog;
import com.surelogic.common.CommonImages;
import com.surelogic.jsure.client.eclipse.Activator;

public final class SendProblemAction implements IWorkbenchWindowActionDelegate {

	public void dispose() {
		// nothing to do
	}

	public void init(IWorkbenchWindow window) {
		// nothing to do
	}

	public void run(IAction action) {
		SendProblemReportDialog.open(SWTUtility.getShell(), 
				"JSure "+EclipseUtility.getVersion(Activator.getDefault()),
				CommonImages.IMG_JSURE_LOGO);
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// nothing to do
	}
}
