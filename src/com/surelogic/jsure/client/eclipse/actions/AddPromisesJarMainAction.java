package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.actions.AbstractMainAction;
import com.surelogic.jsure.client.eclipse.PromisesJarUtility;
import com.surelogic.jsure.client.eclipse.dialogs.JavaProjectSelectionDialog;


public class AddPromisesJarMainAction extends AbstractMainAction {

	public void run(IAction action) {
		IJavaProject focus = JavaProjectSelectionDialog.getProject(
				"Select the project to add/update the promises library:",
				"Add/Update Promises Library", SLImages
						.getImage(CommonImages.IMG_JAR));

		if (focus != null) {
			performAction(focus.getProject());
		}
	}

	public static void performAction(IProject project) {
		if (project != null) {
			PromisesJarUtility.finishProjectSetup(project, true, null);
		}
	}
}
