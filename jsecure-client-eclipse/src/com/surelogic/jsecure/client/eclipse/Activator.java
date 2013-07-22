package com.surelogic.jsecure.client.eclipse;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.surelogic.jsecure.client.eclipse.adhoc.JSecureDataSource;

public class Activator implements BundleActivator {
	// The shared instance
	private static Activator plugin;

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * The constructor
	 */
	public Activator() {
		if (plugin != null) {
			throw new IllegalStateException(Activator.class.getName() + " instance already exits, it should be a singleton.");
		}
		plugin = this;
	}
	
	@Override
	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	    try {
	        JSecureDataSource.getInstance().dispose();
	        plugin = null;
	      } finally {
	    	//extends AbstractUIPlugin {
	    	//super.stop(context);
	      }
	}
}
