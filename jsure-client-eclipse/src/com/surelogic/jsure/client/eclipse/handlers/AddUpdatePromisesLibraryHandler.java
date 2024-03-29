package com.surelogic.jsure.client.eclipse.handlers;

import java.util.List;

import org.eclipse.core.commands.IHandler;
import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.common.CommonImages;
import com.surelogic.common.LibResources;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.dialogs.JavaProjectSelectionDialog;
import com.surelogic.common.ui.handlers.AbstractProjectSelectedMenuHandler;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.jsure.client.eclipse.PromisesJarUtility;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

public class AddUpdatePromisesLibraryHandler extends AbstractProjectSelectedMenuHandler implements IHandler {

  @Override
  public void runActionOn(List<IJavaProject> selectedProjects) {
    for (IJavaProject jp : selectedProjects) {
      SLUIJob job = PromisesJarUtility.getAddUpdatePromisesLibraryUIJob(jp);
      job.schedule();
    }
  }

  @Override
  protected JavaProjectSelectionDialog.Configuration getDialogInfo(List<IJavaProject> selectedProjects) {
    return new JavaProjectSelectionDialog.Configuration(
        "Select project(s) to add or update the " + LibResources.PROMISES_JAR
            + " within so that annotations\ncan be added to the project's code:",
        "Add/Update Promises Library", SLImages.getImage(CommonImages.IMG_JAR), selectedProjects,
        JSurePreferencesUtility.ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_UPDATE_JAR,
        JSurePreferencesUtility.LAST_TIME_PROJECTS_TO_UPDATE_JAR);
  }
}
