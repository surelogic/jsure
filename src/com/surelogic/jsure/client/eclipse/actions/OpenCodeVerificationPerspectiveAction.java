package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.eclipse.ViewUtility;
import com.surelogic.jsure.perspectives.CodeVerificationPerspective;

public class OpenCodeVerificationPerspectiveAction implements
		IWorkbenchWindowActionDelegate {

	@Override
	public void dispose() {
		// Nothing to do
	}

	@Override
	public void init(IWorkbenchWindow window) {
		// Nothing to do
	}

	@Override
	public void run(IAction action) {
		ViewUtility
				.showPerspective(CodeVerificationPerspective.class.getName());
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// Nothing to do
	}
}
