package com.surelogic.jsure.core.serviceability;

import java.io.File;

import com.surelogic.common.jobs.SLStatus;

/**
 * Interface for JSure scan crash reporters.
 * <p>
 * This abstraction is necessary because the Eclipse UI, which is used by the
 * Eclipse JSure client to report scan failures, is not available to the code
 * that runs JSure scans.
 */
public interface IScanCrashReporter {

	/**
	 * Reports a JSure scan crash. Can be invoked from any thread context.
	 * 
	 * @param status
	 *            the status built up about the scan filter. Cannot be
	 *            {@code null}.
	 * @param scanLog
	 *            the log file for the scan within the scan directory. Cannot be
	 *            {@code null}, but reporters should check that the file exists.
	 */
	void reportJSureScanCrash(SLStatus status, File scanLog);

}
