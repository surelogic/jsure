package com.surelogic.jsure.client.eclipse;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.logging.SLEclipseStatusUtility;
import com.surelogic.common.license.SLLicenseProduct;
import com.surelogic.common.ui.DialogTouchNotificationUI;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.jsure.core.driver.JavacDriver;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

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

	/**
	 * The constructor
	 */
	public Activator() {
		if (plugin != null)
			throw new IllegalStateException(Activator.class.getName()
					+ " instance already exits, it should be a singleton.");
		plugin = this;
	}

	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);

		EclipseUIUtility.startup(this);
	}

	// Used for startup
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask("Initializing the JSure tool", 7);

		/*
		 * "Touch" common-core-eclipse so the logging gets Eclipse-ified.
		 */
		SLEclipseStatusUtility.touch(new DialogTouchNotificationUI());
		monitor.worked(1);

		/*
		 * "Touch" the JSure preference initialization.
		 */
		JSurePreferencesUtility.initializeDefaultScope();
		monitor.worked(1);

		EclipseUtility.getProductReleaseDateJob(SLLicenseProduct.JSURE, this)
				.schedule();
		monitor.worked(1);

		SwitchToJSurePerspective.getInstance().init();
		monitor.worked(1);
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		try {
			SwitchToJSurePerspective.getInstance().dispose();
			JavacDriver.getInstance().stopScripting();
		} finally {
			plugin = null;
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

	/**
	 * Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
}
