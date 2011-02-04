package edu.cmu.cs.fluid.dc;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.common.ui.actions.AbstractProjectSelectedMenuAction;
import com.surelogic.common.ui.dialogs.JavaProjectSelectionDialog;
import com.surelogic.jsure.client.eclipse.analysis.JavacBuild;

public class AnalyzeNow2Action extends AbstractProjectSelectedMenuAction {
	protected JavaProjectSelectionDialog.Config getDialogInfo(
			List<IJavaProject> selectedProjects) {
		return new JavaProjectSelectionDialog.Config(
				"Select project(s) to analyze", "Analyze Project", null,
				selectedProjects);
	}

	@Override
	protected void run(List<IJavaProject> selectedProjects,
			List<String> projectNames) {
		// TODO Check if this includes dependencies
		// If not, show confirmation dialog
		JavacBuild.analyze(selectedProjects);
	}
}
