package com.surelogic.jsure.client.eclipse;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.surelogic.common.eclipse.logging.SLStatus;

import edu.cmu.cs.fluid.ide.IDE;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.surelogic.jsure.client.eclipse";

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		/*
     * "Touch" common-eclipse so the logging gets Eclipse-ified.
     */
    SLStatus.touch();

    // TODO find a better place to define this system property
    System.setProperty("derby.storage.pageCacheSize", "2500");
    // startup the database and ensure its schema is up to date
    Data.bootAndCheckSchema();
    
    // TODO reload persistent data
    IDE.getInstance().setReporter(new Reporter());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
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

}
