package com.surelogic.jsure.client.eclipse;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;

import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.jsure.core.JSureUtility;

public class PromisesJarUtility {

  /**
   * Constructs an {@link SLUIJob} that, when executed, interacts with the user
   * to add, if necessary, the promises.jar file into a particular Eclipse Java
   * project.
   * <p>
   * If the passed project is not a Java project then the job exits immediately.
   * <p>
   * A design goal of this job is to avoid user interaction as much as possible.
   * The user has already confirmed the addition of the promises.jar so we don't
   * need to bug them about it. Also we assume that older versions of our
   * library are, in fact, our library so we can delete them from the project.
   * 
   * @param jp
   *          an Eclipse project.
   */
  public static SLUIJob getAddUpdatePromisesLibraryUIJob(final IJavaProject jp) {
    if (jp == null)
      throw new IllegalArgumentException(I18N.err(44, "jp"));
    final String jpName = jp.getProject().getName();
    return new SLUIJob() {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {

        final String javaSourceVersion = JDTUtility.getJavaSourceVersion(jp);
        final int majorJavaSourceVersion = JDTUtility.getMajorJavaSourceVersion(jp);

        if (majorJavaSourceVersion < 5) {
          /*
           * Notify the user that the source level of their project does not
           * allow use of the promises.jar
           */
          MessageDialog.openInformation(EclipseUIUtility.getShell(),
              I18N.msg("jsure.eclipse.dialog.promises.noPromisesJarNeeded.title", jpName),
              I18N.msg("jsure.eclipse.dialog.promises.noPromisesJarNeeded.msg", jpName, javaSourceVersion));
          return Status.OK_STATUS;
        }

        /*
         * The rest of this can be done in a workspace job
         */
        System.out.println(" ***** majorJavaSourceVersion = " + majorJavaSourceVersion);
        final WorkspaceJob wJob = JSureUtility.getJobToAddUpdatePromisesJar(jp, jpName, majorJavaSourceVersion >= 8);
        wJob.schedule();

        return Status.OK_STATUS;
      }
    };
  }
}
