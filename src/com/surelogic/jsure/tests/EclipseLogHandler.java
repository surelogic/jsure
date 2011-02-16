package com.surelogic.jsure.tests;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.XMLFormatter;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import com.surelogic.common.logging.SLLogger;

public class EclipseLogHandler extends Handler {

	private static FileHandler f_fileHandler;

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

		/*
		 * Only output errors and warnings -- drop the other stuff on the floor.
		 */
		if (level == Level.SEVERE || level == Level.WARNING) {
			SLLogger.getLogger().log(level, record.getMessage(),
					record.getThrown());
		}
	}

	public static synchronized String startFileLog(String name) {
		if (f_fileHandler == null) {
			final XMLFormatter xf = new XMLFormatter();
			try {
				final IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace()
						.getRoot();
				final File wsFile = new File(wsRoot.getLocationURI());
				final String logFileName = name + ".xml";
				final File logFile = new File(wsFile, logFileName);
				f_fileHandler = new FileHandler(logFile.getAbsolutePath());
				f_fileHandler.setFormatter(xf);
				f_fileHandler.setLevel(Level.WARNING);
				SLLogger.addHandler(f_fileHandler);
				return logFile.getAbsolutePath();
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
		SLLogger.removeHandler(f_fileHandler);
		f_fileHandler.close();
		f_fileHandler = null;
	}
}
