package com.surelogic.jsure.client.eclipse;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.common.ui.serviceability.SendServiceMessageWizard;
import com.surelogic.scans.serviceability.IScanCrashReporter;

/**
 * An Eclipse implementation of the JSure scan crash reporter. It simply calls
 * {@link SendServiceMessageWizard#openJSureScanCrashReport(SLStatus, File)} in
 * the SWT EDT.
 * <p>
 * There is only one instance needed and it can be obtained by calling
 * {@link #getInstance()}.
 */
public final class EclipseScanCrashReporter implements IScanCrashReporter {

	static private final EclipseScanCrashReporter INSTANCE = new EclipseScanCrashReporter();

	public static IScanCrashReporter getInstance() {
		return INSTANCE;
	}

	private EclipseScanCrashReporter() {
		// singleton
	}

	@Override
	public void reportJSureScanCrash(final SLStatus status, final File scanLog) {
		final UIJob job = new SLUIJob() {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				SendServiceMessageWizard.openJSureScanCrashReport(status,
						scanLog);
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
}
