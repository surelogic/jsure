package com.surelogic.jsure.core;

import java.io.File;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import com.surelogic.common.FileUtility;
import com.surelogic.javac.Util;
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
		
		if (false && !Util.useResultsXML) {
			clearJSureData();
		}
		//monitor.worked(1);
		
		// TODO reload persistent data
		Eclipse.initialize();
		//monitor.worked(1);

		// NotificationHub.addAnalysisListener(ConsistencyListener.prototype);
		JavacDriver.getInstance();		
		//monitor.worked(1);
		
		// Try to update the client's XML
		PromisesLibMerge.updateClient();
	}

	private void clearJSureData() {
		for (File f : JSurePreferencesUtility.getJSureDataDirectory()
				.listFiles()) {
			if (f.isDirectory()) {
				FileUtility.recursiveDelete(f, false);
			}
			// i.e. don't delete persistent files
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