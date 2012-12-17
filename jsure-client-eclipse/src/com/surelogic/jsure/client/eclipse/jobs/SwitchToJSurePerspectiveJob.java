package com.surelogic.jsure.client.eclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.BalloonUtility;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.jsure.client.eclipse.perspectives.CodeVerificationPerspective;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

/**
 * Job to prompt the user to switch to the JSure perspective. It handles all
 * user preferences.
 */
public final class SwitchToJSurePerspectiveJob extends SLUIJob {

  public SwitchToJSurePerspectiveJob() {
    super(SwitchToJSurePerspectiveJob.class.getName());
  }

  @Override
  public IStatus runInUIThread(IProgressMonitor monitor) {
    /*
     * This is run when a scan completes so if we need to show a balloon
     * notification.
     */
    if (EclipseUtility.getBooleanPreference(JSurePreferencesUtility.SHOW_BALLOON_NOTIFICATIONS)) {
      BalloonUtility.showMessage(I18N.msg("jsure.balloon.scandone.title"), I18N.msg("jsure.balloon.scandone.msg"));
    }

    /*
     * Ensure that we are not already in the JSure perspective.
     */
    final boolean inJSurePerspective = EclipseUIUtility.isPerspectiveOpen(CodeVerificationPerspective.class.getName());
    if (inJSurePerspective) {
      return Status.OK_STATUS; // bail
    }

    /*
     * Check that we are the only job of this type running. This is trying to
     * avoid double prompting the user to change to the Flashlight perspective.
     * It may not work in all cases but should eliminate most of them.
     * 
     * In particular if the dialog is already up and the user exits another
     * instrumented program then that exit will trigger another instance of this
     * job to run. Without this check the user would get two prompts to change
     * to the Flashlight perspective.
     */
    final boolean onlySwitchToJSurePerspectiveJobRunning = EclipseUtility
        .getActiveJobCountWithName(SwitchToJSurePerspectiveJob.class.getName()) == 1;
    if (!onlySwitchToJSurePerspectiveJobRunning) {
      return Status.OK_STATUS; // bail
    }

    final boolean change = com.surelogic.jsure.client.eclipse.dialogs.ConfirmPerspectiveSwitch.toCodeVerification(EclipseUIUtility
        .getShell());
    if (change) {
      EclipseUIUtility.showPerspective(CodeVerificationPerspective.class.getName());
    }
    return Status.OK_STATUS;
  }
}
