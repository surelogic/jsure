package com.surelogic.jsure.tests;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.XMLFormatter;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import com.surelogic.common.logging.SLLogger;

public class EclipseLogHandler {

	private static String f_fileName;
	private static MyFileHandler f_fileHandler;

	public static synchronized String startFileLog(String name) {
		if (f_fileHandler == null) {
			try {
				final IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace()
						.getRoot();
				final File wsFile = new File(wsRoot.getLocationURI());
				final String logFileName = name + ".xml";
				final File logFile = new File(wsFile, logFileName);
				f_fileName = logFile.getAbsolutePath();
				f_fileHandler = new MyFileHandler(f_fileName);
				f_fileHandler.setFormatter(new XMLFormatter());
				f_fileHandler.setLevel(Level.ALL);
				SLLogger.addHandler(f_fileHandler);
				return logFile.getAbsolutePath();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			new Exception("Already writing to the log file " + f_fileName)
					.printStackTrace();
		}
		return null;
	}

	public static synchronized boolean stopFileLog() {
		if (f_fileHandler == null)
			return true;
		
		SLLogger.removeHandler(f_fileHandler);
		f_fileHandler.close();
		boolean result = f_fileHandler.logIsOk;
		f_fileHandler = null;
		f_fileName = null;
		return result;
	}
	
	static class MyFileHandler extends FileHandler {
		public MyFileHandler(String path) throws IOException, SecurityException {
			super(path);
		}

		boolean logIsOk = true;
		
		@Override
	    public synchronized void publish(LogRecord record) {
			super.publish(record);
			
			if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
				logIsOk = false;
			}
	    }
	}
}
