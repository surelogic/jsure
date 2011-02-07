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
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			IDE.getInstance().getMemoryPolicy().shutdown();
		} finally {
			super.stop(context);
		}
	}
}