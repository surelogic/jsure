package com.surelogic.jsure.core;

import java.io.File;
import java.util.logging.Level;

import org.eclipse.core.runtime.*;
import org.osgi.framework.BundleContext;

import com.surelogic.common.FileUtility;
import com.surelogic.common.java.JavaProject;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.java.persistence.JSureScan;
import com.surelogic.jsure.core.driver.JSureDriver;
import com.surelogic.jsure.core.driver.JavacEclipse;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.xml.PromisesLibMerge;

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
		JSurePreferencesUtility.getJSureXMLDirectory();

		if (doubleChecker == null) {
			doubleChecker = new com.surelogic.jsure.core.driver.DoubleChecker();
			doubleChecker.start(context);
		}

		if (true) {
			cleanupJSureData();
		}

		// NotificationHub.addAnalysisListener(ConsistencyListener.prototype);
		JavacEclipse.initialize();
		JSureDriver.initInstance(getDriver());

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
		doubleChecker = null;
		super.stop(context);
	}

	public com.surelogic.jsure.core.driver.DoubleChecker getDoubleChecker() {
		return doubleChecker;
	}
	
	@SuppressWarnings({ "unchecked" })
	private JSureDriver<? extends JavaProject> getDriver() {
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IExtensionPoint ep = reg.getExtensionPoint("com.surelogic.jsure.core.jsureDriver");
		IExtension[] extensions = ep.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IExtension ext = extensions[i];
			IConfigurationElement[] ce =
					ext.getConfigurationElements();
			for (int j = 0; j < ce.length; j++) {
				try {
					Object obj = ce[j].createExecutableExtension("class");
					return (JSureDriver<? extends JavaProject>) obj;
				} catch (CoreException e) {
					SLLogger.getLogger().log(Level.SEVERE, "Error while instantiating extension "+ext.getUniqueIdentifier(), e);
					continue;
				}
			}
		}
		return null;
	}
}