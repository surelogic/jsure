package edu.cmu.cs.fluid.eclipse.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import edu.cmu.cs.fluid.ide.IDE;

/**
 * The core Fluid plug-in class adaption the Fluid code base into Eclipse. This
 * plug-in encapsulates the Fluid system. It supports simple adaption of Java
 * information between Eclipse to Fluid and is available to other Eclipse
 * plug-in's to interact with the Fluid code base. <br>
 * Typical use of this plug-in would be in combination with one or more other
 * plug-ins that control and interact with this plug-in. So typically a
 * developer would have this plug-in loaded with another plug-in that controls
 * this plug-in's actions. For example, a plug-in containing a view to allow the
 * programmer to analyze code and view analysis results would use the Fluid
 * plug-in to move the Eclipse AST into the Fluid IR and call Fluid analysis
 * routines to get results to display to the programmer.
 */
public class FluidPlugin extends AbstractUIPlugin {
	/**
	 * The shared Fluid plug-in instance.
	 */
	private static FluidPlugin plugin;

	/**
	 * Returns the shared instance.
	 */
	public static FluidPlugin getDefault() {
		return plugin;
	}

	/**
	 * The constructor.
	 * 
	 */
	public FluidPlugin() {
		plugin = this;
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