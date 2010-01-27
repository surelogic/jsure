package edu.cmu.cs.fluid.dc;

import java.util.logging.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;


import com.surelogic.common.eclipse.actions.AbstractSingleProjectAction;
import com.surelogic.common.logging.SLLogger;

/**
 * Implements a context menu action for IProject and IJavaProject that sets the
 * Fluid nature for the project.
 */
public class FocusFluidNatureAction extends AbstractSingleProjectAction {
	private static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.dc");

	public void run(final IAction action) {
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		boolean changed = false;

		// Remove the nature from those projects that have it
		for (int i = 0; i < projects.length; i++) {
			final IProject current = projects[i];
			// Can only manipulate the nature of open projects
			if (current.isOpen()) {
				if (project != current && Nature.hasNature(current)) {
					try {
						Nature.removeNatureFromProject(current);
						changed = true;
					} catch (CoreException e) {
				    	Majordomo.logError("Error While Removing JSure Nature", 
				    			"Unable to remove JSure nature from Java project "
				    			+ current.getName(), e);
					}
				}
			}
		}

		// Add the nature to the currently selected project
		try {			
			if (!Nature.hasNature(project)) {
				cleanup(); // FIX only for JSure
				Nature.addNatureToProject(project);
			} else if (changed) {
				cleanup(); // FIX only for JSure
				Nature.runAnalysis(project);
			}
		} catch (CoreException e) {
	    	Majordomo.logError("Error While Adding JSure Nature", 
	    			"Unable to add JSure nature from Java project "
	    			+ project.getName(), e);
		}
	}

	protected void cleanup() {
		// Nothing to do
	}
}
