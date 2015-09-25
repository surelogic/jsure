package com.surelogic.jsure.client.eclipse;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.Singleton;
import com.surelogic.ThreadSafe;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.serviceability.scan.IScanCrashReporter;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.common.ui.serviceability.SendServiceMessageWizard;

/**
 * An Eclipse implementation of the JSure scan crash reporter. It simply calls
 * {@link SendServiceMessageWizard#openJSureScanCrashReport(String, SLStatus, File)
 * )} in the SWT EDT.
 * <p>
 * There is only one instance needed and it can be obtained by calling
 * {@link #getInstance()}.
 */
@ThreadSafe
@Singleton
public final class EclipseScanCrashReporter implements IScanCrashReporter {

  static private final EclipseScanCrashReporter INSTANCE = new EclipseScanCrashReporter();

  public static IScanCrashReporter getInstance() {
    return INSTANCE;
  }

  private EclipseScanCrashReporter() {
    // singleton
  }

  @Override
  public void reportScanCrash(final SLStatus status, final File scanLog) {
    final UIJob job = new SLUIJob() {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        SendServiceMessageWizard.openJSureScanCrashReport(SLLicenseProduct.JSURE + " " + EclipseUtility.getSureLogicToolsVersion(),
            status, scanLog);
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  @Override
  public void reportScanCancellation(final String msg) {
    final UIJob job = new SLUIJob() {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        MessageDialog.openInformation(EclipseUIUtility.getShell(), "JSure Scan was Cancelled", msg);
        SLLogger.getLogger().info(msg);
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }
}
