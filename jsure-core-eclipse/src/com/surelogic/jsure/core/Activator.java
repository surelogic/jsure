package com.surelogic.jsure.core;

import java.io.File;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import com.surelogic.common.FileUtility;
import com.surelogic.java.persistence.JSureScan;
import com.surelogic.jsure.core.driver.JavacDriver;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.xml.PromisesLibMerge;

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

		if (true) {
			cleanupJSureData();
		}

		// TODO reload persistent data
		Eclipse.initialize();

		// NotificationHub.addAnalysisListener(ConsistencyListener.prototype);
		JavacDriver.getInstance();

		// Clean out empty paths in client's XML promises
		PromisesLibMerge.removeEmptyPathsOnClient();

		// Try to update the client's XML
		PromisesLibMerge.mergeJSureToLocal();
	}

	private void cleanupJSureData() {
		for (File f : JSurePreferencesUtility.getJSureDataDirectory()
				.listFiles()) {
			if (f.isDirectory()) {
				if (JSureScan.isIncompleteScan(f)) {
					FileUtility.recursiveDelete(f, false);
				}
			}
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