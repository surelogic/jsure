package com.surelogic.jsure.tests;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 */
public class Activator extends Plugin {

	// The shared instance
	private static Activator plugin;

	private static ILog log = null;

	/**
	 * The constructor
	 */
	public Activator() {
		plugin = this;
		log = getLog();
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Gets the shared plug-in instance.
	 * 
	 * @return the shared plug-in instance.
	 */
	public static Activator getDefault() {
		return plugin;
	}

	static ILog getILog() {
		return log;
	}
}
