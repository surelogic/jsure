package com.surelogic.jsure.core;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

import edu.cmu.cs.fluid.ide.IDE;

public class Activator extends Plugin {

	/**
	 * The shared Fluid plug-in instance.
	 */
	private static Activator plugin;

	private com.surelogic.jsure.core.driver.DoubleChecker doubleChecker;
	
	/**
	 * Returns the shared instance.
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * The constructor.
	 * 
	 */
	public Activator() {
		plugin = this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		JSurePreferencesUtility.initializeDefaultScope();
		
		if (doubleChecker == null) {
			doubleChecker = new com.surelogic.jsure.core.driver.DoubleChecker();
			doubleChecker.start(context);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			IDE.getInstance().getMemoryPolicy().shutdown();
		} finally {
			doubleChecker = null;
			super.stop(context);
		}
	}
	
	public com.surelogic.jsure.core.driver.DoubleChecker getDoubleChecker() {
		return doubleChecker;
	}
}