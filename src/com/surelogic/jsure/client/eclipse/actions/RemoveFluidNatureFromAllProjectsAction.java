package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.jsure.client.eclipse.listeners.ClearProjectListener;

public class RemoveFluidNatureFromAllProjectsAction implements
		IWorkbenchWindowActionDelegate {

	public void dispose() {
		// Nothing to do
	}

	public void init(IWorkbenchWindow window) {
		// Nothing to do
	}

	public void run(IAction action) {
		ClearProjectListener.clearNatureFromAllOpenProjects();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// Nothing to do
	}
}