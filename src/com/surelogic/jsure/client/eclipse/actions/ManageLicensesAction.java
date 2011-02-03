package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.ui.SWTUtility;
import com.surelogic.common.ui.dialogs.ManageLicensesDialog;

public final class ManageLicensesAction implements
		IWorkbenchWindowActionDelegate {

	public void dispose() {
		// nothing to do
	}

	public void init(IWorkbenchWindow window) {
		// nothing to do
	}

	public void run(IAction action) {
		ManageLicensesDialog.open(SWTUtility.getShell());
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// nothing to do
	}
}
