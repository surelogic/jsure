package com.surelogic.jsure.client.eclipse.actions;

import java.util.logging.Level;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.client.eclipse.listeners.ClearProjectListener;

import edu.cmu.cs.fluid.dc.Nature;

public class RemoveFluidNatureFromAllProjectsAction implements
		IWorkbenchWindowActionDelegate {

	public void dispose() {
		// Nothing to do
	}

	public void init(IWorkbenchWindow window) {
		// Nothing to do
	}

	public void run(IAction action) {
		// Handle projects that are still active
		final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		if (projects == null)
			return;

		for (IProject p : projects) {
			if (p.isOpen() && Nature.hasNature(p)) {
				try {
					Nature.removeNatureFromProject(p);
				} catch (CoreException e) {
					SLLogger.getLogger().log(
							Level.SEVERE,
							"CoreException trying to remove the JSure nature from "
									+ p.getName(), e);
				}
			}
		}

		ClearProjectListener.postNatureChangeUtility();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// Nothing to do
	}
}