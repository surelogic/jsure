package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.jsure.client.eclipse.listeners.ClearProjectListener;


public class ClearDropSeaAction implements IWorkbenchWindowActionDelegate {
	IWorkbenchWindow window;

	public void dispose() {
		// TODO Auto-generated method stub

	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void run(IAction action) {
		if (MessageDialog.openConfirm(window.getShell(),
				"Wipe all analysis results",
				"Are you sure you want to wipe all results, "
				+ "i.e., are all Java projects closed?")) {
			ClearProjectListener.clearDropSea();			
			// FIX refreshView();
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

}
