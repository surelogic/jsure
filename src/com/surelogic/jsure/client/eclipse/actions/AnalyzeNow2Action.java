package com.surelogic.jsure.client.eclipse.actions;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.common.logging.SLLogger;
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

		/*
		 * License check: A hack because JSure is not using SLJobs yet.
		 */
		final SLStatus failed = SLLicenseUtility.validateSLJob(
				SLLicenseProduct.JSURE, new NullSLProgressMonitor());
		if (failed != null) {
			SLLogger.getLogger().log(failed.getSeverity().toLevel(),
					failed.getMessage(), failed.getException());
			return;
		}

		JavacBuild.analyze(selectedProjects);
	}
}
