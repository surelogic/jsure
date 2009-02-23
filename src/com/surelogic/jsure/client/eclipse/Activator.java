package com.surelogic.jsure.client.eclipse;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.surelogic.common.eclipse.logging.SLEclipseStatusUtility;

import edu.cmu.cs.fluid.analysis.util.ConsistencyListener;
import edu.cmu.cs.fluid.dc.NotificationHub;
import edu.cmu.cs.fluid.dc.Plugin;
import edu.cmu.cs.fluid.eclipse.Eclipse;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.surelogic.jsure.client.eclipse";

	// The shared instance
	private static Activator plugin;

	//Resource bundle.
	//private ResourceBundle resourceBundle;

	private Plugin doubleChecker;
	
	/**
	 * The constructor
	 */
	public Activator() {
		// Do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		if (doubleChecker == null) {
			doubleChecker = new Plugin();
			doubleChecker.start(context);
		}
		
		/*
		try {
			resourceBundle = 
				ResourceBundle.getBundle("edu.cmu.cs.fluid.dcf.PluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		*/
		
		/*
		 * "Touch" common-eclipse so the logging gets Eclipse-ified.
		 */
		SLEclipseStatusUtility.touch();

		// startup the database and ensure its schema is up to date
		Data.getInstance().bootAndCheckSchema();

		// TODO reload persistent data
		Eclipse.initialize();
		
		NotificationHub.addAnalysisListener(ConsistencyListener.prototype);
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
		plugin = null;
		try {
			// TODO save persistent data
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
