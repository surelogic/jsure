package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.SWTUtility;
import com.surelogic.common.eclipse.dialogs.InstallTutorialProjectsDialog;

public class ImportTutorialProjectsAction implements
		IWorkbenchWindowActionDelegate {

	public void dispose() {
		// Do nothing
	}

	public void init(final IWorkbenchWindow window) {
		// Do nothing

	}

	public void run(final IAction action) {
		final ClassLoader loader = Thread.currentThread()
				.getContextClassLoader();
		InstallTutorialProjectsDialog.open(SWTUtility.getShell(),
				CommonImages.IMG_JSURE_LOGO,
				"/com.surelogic.jsure.client.help/ch01s03.html", loader
						.getResource("/lib/PlanetBaronJSure.zip"), loader
						.getResource("/lib/BoundedFIFOJSure.zip"));
	}

	public void selectionChanged(final IAction action,
			final ISelection selection) {
		// Do nothing

	}

}
