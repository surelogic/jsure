package com.surelogic.jsure.client.eclipse.actions;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.common.CommonImages;
import com.surelogic.common.LibResources;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.actions.AbstractProjectSelectedMenuAction;
import com.surelogic.common.ui.dialogs.JavaProjectSelectionDialog;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.jsure.client.eclipse.PromisesJarUtility;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

public class AddUpdatePromisesLibraryAction extends
		AbstractProjectSelectedMenuAction {
	@Override
	protected void runActionOn(final List<IJavaProject> selectedProjects) {

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

		for (IJavaProject jp : selectedProjects) {
			System.out
					.println("scheduled job for " + jp.getProject().getName());
			SLUIJob job = PromisesJarUtility
					.getAddUpdatePromisesLibraryUIJob(jp);
			job.schedule();
		}
	}

	@Override
	protected JavaProjectSelectionDialog.Configuration getDialogInfo(
			List<IJavaProject> selectedProjects) {
		return new JavaProjectSelectionDialog.Configuration(
				"Select project(s) to add or update the "
						+ LibResources.PROMISES_JAR
						+ " within so that annotations\ncan be added to the project's code:",
				"Add/Update Promises Library",
				SLImages.getImage(CommonImages.IMG_JSURE_VERIFY),
				selectedProjects,
				JSurePreferencesUtility.ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_SCAN,
				null);
	}
}