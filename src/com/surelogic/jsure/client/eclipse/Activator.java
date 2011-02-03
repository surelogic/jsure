package com.surelogic.jsure.client.eclipse;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.surelogic.common.FileUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.ui.SWTUtility;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.ui.DialogTouchNotificationUI;
import com.surelogic.fluid.eclipse.preferences.PreferenceConstants;
import com.surelogic.fluid.javac.Util;
import com.surelogic.jsure.client.eclipse.analysis.JavacDriver;

import edu.cmu.cs.fluid.dc.Plugin;
import edu.cmu.cs.fluid.eclipse.Eclipse;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements
		IRunnableWithProgress {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.surelogic.jsure.client.eclipse";

	// The shared instance
	private static Activator plugin;

	// Resource bundle.
	// private ResourceBundle resourceBundle;

	private Plugin doubleChecker;

	/**
	 * The constructor
	 */
	public Activator() {
		// Do nothing
	}

	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		if (doubleChecker == null) {
			doubleChecker = new Plugin();
			doubleChecker.start(context);
		}
		SWTUtility.startup(this);
	}

	// Used for startup
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask("Initializing the JSure tool", 6);
		
		/*
		 * "Touch" common-core-eclipse so the logging gets Eclipse-ified.
		 */
		SLEclipseStatusUtility.touch(new DialogTouchNotificationUI());
		monitor.worked(1);

		if (!Util.useResultsXML) {
			clearJSureData();
			monitor.worked(1);
		}
		
		// TODO reload persistent data
		Eclipse.initialize();
		monitor.worked(1);

		EclipseUtility.getProductReleaseDateJob(SLLicenseProduct.JSURE, this)
				.schedule();
		monitor.worked(1);

		//NotificationHub.addAnalysisListener(ConsistencyListener.prototype);
		JavacDriver.getInstance();
		monitor.worked(1);
	}

	private void clearJSureData() {
		for (File f : PreferenceConstants.getJSureDataDirectory().listFiles()) {
			if (f.isDirectory()) {
				FileUtility.recursiveDelete(f, false);
			}
			// i.e. don't delete persistent files
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		try {
			JavacDriver.getInstance().stopScripting();
			plugin = null;
		} finally {
			super.stop(context);
		}
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public String getLocation(final String loc) {
		final IPath pluginState = getStateLocation();
		return pluginState.toOSString() + System.getProperty("file.separator")
				+ loc;
	}

	/**
	 * Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	public Plugin getDoubleChecker() {
		return doubleChecker;
	}
}
