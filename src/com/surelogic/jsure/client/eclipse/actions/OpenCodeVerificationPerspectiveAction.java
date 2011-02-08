package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.jsure.client.eclipse.perspectives.CodeVerificationPerspective;

public class OpenCodeVerificationPerspectiveAction implements
		IWorkbenchWindowActionDelegate {

	public void dispose() {
		// Nothing to do
	}

	public void init(IWorkbenchWindow window) {
		// Nothing to do
	}

	public void run(IAction action) {
		EclipseUIUtility.showPerspective(CodeVerificationPerspective.class
				.getName());
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// Nothing to do
	}
}
