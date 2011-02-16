package com.surelogic.jsure.tests;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.XMLFormatter;

import com.surelogic.common.logging.SLLogger;

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

		if (level == Level.SEVERE || level == Level.WARNING) {
			SLLogger.getLogger().log(level, record.getMessage(),
					record.getThrown());
		}
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
				SLLogger.addHandler(fileLog);
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
		SLLogger.removeHandler(fileLog);
		fileLog.close();
		fileLog = null;
	}
}
