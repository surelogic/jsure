package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.dialogs.InstallTutorialProjectsDialog;

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
		InstallTutorialProjectsDialog.open(EclipseUIUtility.getShell(),
				CommonImages.IMG_JSURE_LOGO,
				"/com.surelogic.jsure.client.help/ch01s03.html",
				loader.getResource("/lib/PlanetBaronJSure.zip"),
				loader.getResource("/lib/BoundedFIFOJSure.zip"),
				loader.getResource("/lib/oswego.util.concurrent.zip"),
        loader.getResource("/lib/SimpleVariable.zip"),
        loader.getResource("/lib/UniqueAndEffects.zip"),
        loader.getResource("/lib/AdvancedEffectExamples.zip"));
	}

	public void selectionChanged(final IAction action,
			final ISelection selection) {
		// Do nothing

	}

}
