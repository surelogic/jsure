package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.jsure.client.eclipse.dialogs.JavaProjectSelectionDialog;

import edu.cmu.cs.fluid.dc.PromisesJarUtility;

public class AddPromisesJarMainAction implements IWorkbenchWindowActionDelegate {

	public void dispose() {
		// Nothing to do
	}

	public void init(IWorkbenchWindow window) {
		// Nothing to do
	}

	public void run(IAction action) {
		IJavaProject focus = JavaProjectSelectionDialog.getProject(
				"Select the project to add/update the promises library:",
				"Add/Update Promises Library", SLImages
						.getImage(CommonImages.IMG_JAR));

		if (focus != null) {
			performAction(focus.getProject());
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// Nothing to do
	}

	public static void performAction(IProject project) {
		if (project != null) {
			PromisesJarUtility.finishProjectSetup(project, true, null);
		}
	}
}
