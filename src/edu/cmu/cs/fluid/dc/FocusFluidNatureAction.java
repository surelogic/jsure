package edu.cmu.cs.fluid.dc;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;

import com.surelogic.common.logging.SLLogger;

/**
 * Implements a context menu action for IProject and IJavaProject that sets the
 * Fluid nature for the project.
 */
public class FocusFluidNatureAction implements IViewActionDelegate,
		IObjectActionDelegate {
	private static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.dc");

	private IProject project;

	public void setActivePart(final IAction action,
			final IWorkbenchPart targetPart) {
		// do nothing
	}

	public void init(final IViewPart view) {
		// do nothing
	}

	public void selectionChanged(final IAction action,
			final ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object obj = (((IStructuredSelection) selection).getFirstElement());
			if (obj != null) {
				project = (IProject) ((IAdaptable) obj)
						.getAdapter(IProject.class);
			} else {
				project = null;
			}
		}
	}

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
						LOG.log(Level.SEVERE,
								"failure while removing double-checking nature to Java project "
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
			LOG.log(Level.SEVERE,
					"failure while adding double-checking nature to Java project "
							+ project.getName(), e);
		}
	}

	protected void cleanup() {
		// Nothing to do
	}
}
