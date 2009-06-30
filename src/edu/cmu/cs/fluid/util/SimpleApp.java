package edu.cmu.cs.fluid.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

public class SimpleApp {

	public static Logger thisApp = null;

	/**
	 * Runs various setup routines and configures 'thisApp' to be the category
	 * for this application.
	 */
	public static QuickProperties configure(String appName) {
		if (thisApp != null) {
			thisApp.info("Already configured for " + appName);
		}

		QuickProperties qp = QuickProperties.getInstance();
		qp.loadPropertyFile(appName);

		thisApp = SLLogger.getLogger(appName);

		return qp;
	}

	public static Object logInit(Logger log, String init) {
		QuickProperties.getInstance();

		if (log.isLoggable(Level.FINE))
			log.fine("Initializing " + init);
		return null;
	}

	public static Object logInit(Logger log, String init, Throwable t) {
		QuickProperties.getInstance();
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Initializing " + init, t);
		return null;
	}
}
