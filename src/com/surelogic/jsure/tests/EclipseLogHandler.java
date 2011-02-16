/*
 * Created on Jul 9, 2004
 *
 */
package com.surelogic.jsure.tests;

import java.io.*;
import java.util.logging.*;

import org.eclipse.core.runtime.IStatus;

import com.surelogic.common.logging.SLLogger;

/**
 * @author Edwin
 * 
 */
public class EclipseLogHandler extends Handler {
	public static final EclipseLogHandler prototype = new EclipseLogHandler();
	private static FileHandler fileLog;

	@Override
	public void close() throws SecurityException {
		// does nothing
	}

	@Override
	public void flush() {
		// does nothing
	}

	@Override
	public void publish(final LogRecord record) {
		final Level level = record.getLevel();

		int severity = IStatus.INFO;
		if (level == Level.WARNING) {
			severity = IStatus.WARNING;

			// ignored
			return;
		} else if (level == Level.SEVERE) {
			severity = IStatus.ERROR;
		} else {
			// Ignored
			return;
		}
		// ErrorLog.elog(severity, record.getMessage(), record.getThrown());
	}

	public static synchronized void init() {
		final Logger log = SLLogger.getLogger("");
		for (Handler h : log.getHandlers()) {
			if (prototype == h) {
				// Already registered
				return;
			}
		}
		log.addHandler(prototype);
	}

	public static synchronized String startFileLog(String name) {
		if (fileLog == null) {
			final XMLFormatter xf = new XMLFormatter();
			try {
				String file = name + ".xml";
				File f = new File(file);
				fileLog = new FileHandler(file);
				fileLog.setFormatter(xf);
				fileLog.setLevel(Level.WARNING);
				SLLogger.getLogger("").addHandler(fileLog);
				return f.getAbsolutePath();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			new Throwable("Already created a file log").printStackTrace();
		}
		return null;
	}

	public static synchronized void stopFileLog() {
		SLLogger.getLogger("").removeHandler(fileLog);
		fileLog.close();
		fileLog = null;
	}
}
