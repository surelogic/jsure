package com.surelogic.jsure.client.eclipse.actions;

import java.util.logging.Level;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;

import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.eclipse.actions.AbstractMainAction;
import com.surelogic.common.CommonImages;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.client.eclipse.analysis.JavacDriver;
import com.surelogic.jsure.client.eclipse.analysis.ScriptCommands;
import com.surelogic.jsure.client.eclipse.dialogs.JavaProjectSelectionDialog;
import com.surelogic.jsure.client.eclipse.listeners.ClearProjectListener;

import edu.cmu.cs.fluid.dc.Nature;

public class FocusVerificationMainAction extends AbstractMainAction {
	public void run(IAction action) {
		IJavaProject focus = JavaProjectSelectionDialog.getProject(
				"Select the project to verify:", "Focus Verification", SLImages
						.getImage(CommonImages.IMG_JSURE_VERIFY));

		if (focus != null && !Nature.hasNature(focus.getProject())) {
			for(IProject p : ClearProjectListener.clearNatureFromAllOpenProjects()) {
				JavacDriver.getInstance().recordProjectAction(ScriptCommands.REMOVE_NATURE, p);
			}
			try {
				Nature.addNatureToProject(focus.getProject());
				JavacDriver.getInstance().recordProjectAction(ScriptCommands.ADD_NATURE, focus.getProject());
			} catch (CoreException e) {
				SLLogger.getLogger().log(Level.SEVERE,
						"Failure adding JSure nature to " + focus, e);
			}
			ClearProjectListener.postNatureChangeUtility();
		}

	}
}
