package edu.cmu.cs.fluid.eclipse.logging;

import java.util.Dictionary;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.*;

import edu.cmu.cs.fluid.eclipse.ui.FluidPlugin;

/**
 * Modified from code in double-checker Plugin
 */
public class ErrorLog {
	/**
	 * Sends a log message to the Eclipse error log. This log is visible within
	 * the IDE via the built in <i>PDE Runtime Error Log</i> view. This method
	 * is a wrapper to simplify the Eclipse API for reporting to this log.
	 * 
	 * @param severity
	 *            one of <code>IStatus.OK</code>,<code>IStatus.ERROR</code>,
	 *            <code>IStatus.INFO</code>, or <code>IStatus.WARNING</code>
	 *            (from {@link org.eclipse.core.runtime.IStatus})
	 * @param logMessage
	 *            a human-readable message
	 * 
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String, Throwable)
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String, Throwable)
	 */
	public static void elog(int severity, String logMessage) {
		elog(FluidPlugin.getDefault(), severity, logMessage, null);
	}

	/**
	 * Sends a log message to the Eclipse error log. This log is visible within
	 * the IDE via the built in <i>PDE Runtime Error Log</i> view. This method
	 * is a wrapper to simplify the Eclipse API for reporting to this log.
	 * 
	 * @param severity
	 *            one of <code>IStatus.OK</code>,<code>IStatus.ERROR</code>,
	 *            <code>IStatus.INFO</code>, or <code>IStatus.WARNING</code>
	 *            (from {@link org.eclipse.core.runtime.IStatus})
	 * @param logMessage
	 *            a human-readable message
	 * @param exception
	 *            a low-level exception, or <code>null</code> if not applicable
	 * 
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String)
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String, Throwable)
	 */
	public static void elog(int severity, String logMessage, Throwable exception) {
		elog(FluidPlugin.getDefault(), severity, logMessage, exception);
	}

	/**
	 * Displays an error dialog to the user and logs a issue to the Eclipse log.
	 * The error dialog will include detailed plugin information. The logged
	 * message is visible within the IDE via the built in <i>PDE Runtime Error
	 * Log</i> view. This method is a wrapper to simplify the Eclipse API for
	 * reporting to the user and the log.
	 * 
	 * @param severity
	 *            one of <code>IStatus.OK</code>,<code>IStatus.ERROR</code>,
	 *            <code>IStatus.INFO</code>, or <code>IStatus.WARNING</code>
	 *            (from {@link org.eclipse.core.runtime.IStatus})
	 * @param logMessage
	 *            a human-readable message
	 * @param dialogTitle
	 *            a human-readable title for the UI dialog
	 * @param dialogMessage
	 *            a human-readable message to describe the issue to the user
	 *            within the dialog
	 * 
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String, Throwable)
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String)
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String, Throwable)
	 */
	public static void elogPrompt(int severity, String logMessage,
			String dialogTitle, String dialogMessage) {
		elogPrompt(FluidPlugin.getDefault(), severity, logMessage, dialogTitle,
				dialogMessage, null);
	}

	/**
	 * Displays an error dialog to the user and logs a issue to the Eclipse log.
	 * The error dialog will include detailed plugin information and any
	 * exception information provided. The logged message is visible within the
	 * IDE via the built in <i>PDE Runtime Error Log</i> view. This method is a
	 * wrapper to simplify the Eclipse API for reporting to the user and the
	 * log.
	 * 
	 * @param severity
	 *            one of <code>IStatus.OK</code>,<code>IStatus.ERROR</code>,
	 *            <code>IStatus.INFO</code>, or <code>IStatus.WARNING</code>
	 *            (from {@link org.eclipse.core.runtime.IStatus})
	 * @param logMessage
	 *            a human-readable message
	 * @param dialogTitle
	 *            a human-readable title for the UI dialog
	 * @param dialogMessage
	 *            a human-readable message to describe the issue to the user
	 *            within the dialog
	 * @param exception
	 *            a low-level exception, or <code>null</code> if not applicable
	 * 
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String)
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String)
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String, Throwable)
	 */
	public static void elogPrompt(final int severity, final String logMessage,
			final String dialogTitle, final String dialogMessage,
			final Throwable exception) {
		elogPrompt(FluidPlugin.getDefault(), severity, logMessage, dialogTitle,
				dialogMessage, exception);
	}

	//
	// ECLIPSE ERROR REPORTING METHODS (VISIBLE THROUGH THE ECLIPSE UI)
	//

	/**
	 * Sends a log message to the Eclipse error log. This log is visible within
	 * the IDE via the built in <i>PDE Runtime Error Log</i> view. This method
	 * is a wrapper to simplify the Eclipse API for reporting to this log.
	 * 
	 * @param from
	 *            the Eclipse plugin sending the log, or <code>null</code> if
	 *            the plugin cannot be determined
	 * @param severity
	 *            one of <code>IStatus.OK</code>,<code>IStatus.ERROR</code>,
	 *            <code>IStatus.INFO</code>, or <code>IStatus.WARNING</code>
	 *            (from {@link org.eclipse.core.runtime.IStatus})
	 * @param logMessage
	 *            a human-readable message
	 * 
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String, Throwable)
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String, Throwable)
	 */
	public static void elog(org.eclipse.core.runtime.Plugin from, int severity,
			String logMessage) {
		elog(from, severity, logMessage, null);
	}

	/**
	 * Sends a log message to the Eclipse error log. This log is visible within
	 * the IDE via the built in <i>PDE Runtime Error Log</i> view. This method
	 * is a wrapper to simplify the Eclipse API for reporting to this log.
	 * 
	 * @param from
	 *            the Eclipse plugin sending the log, or <code>null</code> if
	 *            the plugin cannot be determined
	 * @param severity
	 *            one of <code>IStatus.OK</code>,<code>IStatus.ERROR</code>,
	 *            <code>IStatus.INFO</code>, or <code>IStatus.WARNING</code>
	 *            (from {@link org.eclipse.core.runtime.IStatus})
	 * @param logMessage
	 *            a human-readable message
	 * @param exception
	 *            a low-level exception, or <code>null</code> if not applicable
	 * 
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String)
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String, Throwable)
	 */
	public static void elog(org.eclipse.core.runtime.Plugin from, int severity,
			String logMessage, Throwable exception) {
		// build up log information
		if (from == null) {
			from = FluidPlugin.getDefault(); // default to this plugin if none
												// was
			// provided
		}
		Status logContent = new Status(severity, from.getBundle()
				.getSymbolicName(), severity, logMessage, exception);
		FluidPlugin.getDefault().getLog().log(logContent);
	}

	/**
	 * Displays an error dialog to the user and logs a issue to the Eclipse log.
	 * The error dialog will include detailed plugin information. The logged
	 * message is visible within the IDE via the built in <i>PDE Runtime Error
	 * Log</i> view. This method is a wrapper to simplify the Eclipse API for
	 * reporting to the user and the log.
	 * 
	 * @param from
	 *            the Eclipse plugin sending the message, or <code>null</code>
	 *            if the plugin cannot be determined
	 * @param severity
	 *            one of <code>IStatus.OK</code>,<code>IStatus.ERROR</code>,
	 *            <code>IStatus.INFO</code>, or <code>IStatus.WARNING</code>
	 *            (from {@link org.eclipse.core.runtime.IStatus})
	 * @param logMessage
	 *            a human-readable message
	 * @param dialogTitle
	 *            a human-readable title for the UI dialog
	 * @param dialogMessage
	 *            a human-readable message to describe the issue to the user
	 *            within the dialog
	 * 
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String, Throwable)
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String)
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String, Throwable)
	 */
	public static void elogPrompt(org.eclipse.core.runtime.Plugin from,
			int severity, String logMessage, String dialogTitle,
			String dialogMessage) {
		elogPrompt(from, severity, logMessage, dialogTitle, dialogMessage, null);
	}

	/**
	 * Displays an error dialog to the user and logs a issue to the Eclipse log.
	 * The error dialog will include detailed plugin information and any
	 * exception information provided. The logged message is visible within the
	 * IDE via the built in <i>PDE Runtime Error Log</i> view. This method is a
	 * wrapper to simplify the Eclipse API for reporting to the user and the
	 * log.
	 * 
	 * @param from
	 *            the Eclipse plugin sending the message, or <code>null</code>
	 *            if the plugin cannot be determined
	 * @param severity
	 *            one of <code>IStatus.OK</code>,<code>IStatus.ERROR</code>,
	 *            <code>IStatus.INFO</code>, or <code>IStatus.WARNING</code>
	 *            (from {@link org.eclipse.core.runtime.IStatus})
	 * @param logMessage
	 *            a human-readable message
	 * @param dialogTitle
	 *            a human-readable title for the UI dialog
	 * @param dialogMessage
	 *            a human-readable message to describe the issue to the user
	 *            within the dialog
	 * @param exception
	 *            a low-level exception, or <code>null</code> if not applicable
	 * 
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String)
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String)
	 * @see #elog(org.eclipse.core.runtime.Plugin, int, String, Throwable)
	 */
	public static void elogPrompt(final org.eclipse.core.runtime.Plugin from,
			final int severity, final String logMessage,
			final String dialogTitle, final String dialogMessage,
			final Throwable exception) {
		// log the issue
		elog(from, severity, logMessage, exception);
		// need to update our view (if it still exists)
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				// build up dialog information
				org.eclipse.core.runtime.Plugin fromPlugin = from;
				if (fromPlugin == null) {
					fromPlugin = FluidPlugin.getDefault();
					// default to this plugin if none was provided
				}
				MultiStatus dialogContent = new MultiStatus(fromPlugin
						.getBundle().getSymbolicName(), severity, logMessage,
						exception);
				addServiceInfo(dialogContent, fromPlugin);
				dialogContent.add(new Status(severity, fromPlugin.getBundle()
						.getSymbolicName(), severity, "Problem: " + logMessage,
						exception));
				// add exception information to the dialog if any exists
				if (exception != null) {
					dialogContent.add(new Status(severity, fromPlugin
							.getBundle().getSymbolicName(), severity, "> "
							+ exception.getClass().getName()
							+ " thrown"
							+ (exception.getMessage() != null ? " ["
									+ exception.getMessage() + "]" : ""),
							exception));
					for (int i = 0; i < exception.getStackTrace().length; i++) {
						dialogContent.add(new Status(severity, fromPlugin
								.getBundle().getSymbolicName(), severity, "> "
								+ exception.getStackTrace()[i], exception));
					}
				}
				// prompt the user
				ErrorDialog.openError((Shell) null, dialogTitle, dialogMessage,
						dialogContent);
			}
		});
	}

	/**
	 * Adds plugin information to a {@link MultiStatus}object for use in an
	 * error dialog. This routine simply provides support the
	 * <code>elogPrompt</code> methods.
	 * 
	 * @param dialogContent
	 *            the
	 * @{link MultiStatus} object to add information to
	 * @param from
	 *            the plugin to extract the information from
	 * 
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String)
	 * @see #elogPrompt(org.eclipse.core.runtime.Plugin, int, String, String,
	 *      String, Throwable)
	 */
	private static void addServiceInfo(MultiStatus dialogContent,
			org.eclipse.core.runtime.Plugin from) {

		Dictionary<String, ?> headers = from.getBundle().getHeaders();

		dialogContent
				.add(new Status(
						IStatus.INFO,
						from.getBundle().getSymbolicName(),
						IStatus.INFO,
						"Plug-in Provider: "
								+ headers
										.get(org.osgi.framework.Constants.BUNDLE_VENDOR),
						null));
		dialogContent.add(new Status(IStatus.INFO, from.getBundle()
				.getSymbolicName(), IStatus.INFO, "Plug-in Name: "
				+ headers.get(org.osgi.framework.Constants.BUNDLE_NAME), null));
		dialogContent.add(new Status(IStatus.INFO, from.getBundle()
				.getSymbolicName(), IStatus.INFO, "Plug-in ID: "
				+ from.getBundle().getSymbolicName(), null));
		dialogContent.add(new Status(IStatus.INFO, from.getBundle()
				.getSymbolicName(), IStatus.INFO, "Version: "
				+ headers.get(org.osgi.framework.Constants.BUNDLE_VERSION),
				null));
	}
}
