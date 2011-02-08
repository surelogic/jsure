package com.surelogic.jsure.client.eclipse.actions;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.dialogs.JavaProjectSelectionDialog;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

public class NewScanDialogAction extends NewScanAction {
	@Override
	protected JavaProjectSelectionDialog.Configuration getDialogInfo(
			List<IJavaProject> selectedProjects) {
		return new JavaProjectSelectionDialog.Configuration(
				"Select project(s) to scan:",
				"Scan Project",
				SLImages.getImage(CommonImages.IMG_SIERRA_SCAN),
				selectedProjects,
				JSurePreferencesUtility.ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_SCAN);
	}
}
