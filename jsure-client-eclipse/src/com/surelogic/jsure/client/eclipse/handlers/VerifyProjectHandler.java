package com.surelogic.jsure.client.eclipse.handlers;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.java.JavaBuild;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.NullSLProgressMonitor;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.license.SLLicenseUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.BalloonUtility;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.dialogs.JavaProjectSelectionDialog;
import com.surelogic.common.ui.dialogs.SaveDirtyFilesUtility;
import com.surelogic.common.ui.dialogs.SaveDirtyFilesUtility.Config;
import com.surelogic.common.ui.handlers.AbstractProjectSelectedMenuHandler;
import com.surelogic.jsure.core.driver.JavacDriver;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

public final class VerifyProjectHandler extends
		AbstractProjectSelectedMenuHandler {

	@Override
	protected void runActionOn(final List<IJavaProject> selectedProjects) {
		verify(selectedProjects);
	}
	
	public static void verify(final List<IJavaProject> selectedProjects) {
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
		final SLJob job = new AbstractSLJob("Checking for builds in progress") {
			@Override
			public SLStatus run(SLProgressMonitor monitor) {
				JavacDriver.waitForBuild();

				final boolean okay = JavaBuild.analyze(JavacDriver.getInstance(), selectedProjects,
						BalloonUtility.errorListener);
				if (okay) {
					showStartBalloon();
				}
				return SLStatus.OK_STATUS;
			}
		};
		Runnable r = new Runnable() {
			@Override
			public void run() {
				final IProject[] projects = new IProject[selectedProjects
						.size()];
				for (int i = 0; i < projects.length; i++) {
					projects[i] = selectedProjects.get(i).getProject();
				}
				final boolean before = EclipseUtility
						.getBooleanPreference(JSurePreferencesUtility.SAVE_DIRTY_EDITORS_BEFORE_VERIFY);
				Config c = new Config(
						"Save and Verify",
						I18N.msg("jsure.eclipse.preference.page.autoSaveBeforeVerify"),
						before);
				boolean scan = SaveDirtyFilesUtility.saveDirtyResources(projects, c);
				if (c.getAlwaysSavePref() != before) {
					EclipseUtility
							.setBooleanPreference(
									JSurePreferencesUtility.SAVE_DIRTY_EDITORS_BEFORE_VERIFY,
									c.getAlwaysSavePref());
				}
				if (scan) {
				  final Job eJob = EclipseUtility.toEclipseJob(job);
				  eJob.schedule();
				}
			}
		};
		EclipseUIUtility.asyncExec(r);
	}

	@Override
	protected JavaProjectSelectionDialog.Configuration getDialogInfo(
			List<IJavaProject> selectedProjects) {
		return new JavaProjectSelectionDialog.Configuration(
				"Select project(s) to verify:",
				"Verify Project",
				SLImages.getImage(CommonImages.IMG_JSURE_VERIFY),
				selectedProjects,
				JSurePreferencesUtility.ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_SCAN,
				JSurePreferencesUtility.LAST_TIME_PROJECTS_TO_SCAN);
	}

	private static void showStartBalloon() {
		if (EclipseUtility
				.getBooleanPreference(JSurePreferencesUtility.SHOW_BALLOON_NOTIFICATIONS)) {
			BalloonUtility.showMessage(
					I18N.msg("jsure.balloon.scanstart.title"),
					I18N.msg("jsure.balloon.scanstart.msg"));
		}
	}
}
