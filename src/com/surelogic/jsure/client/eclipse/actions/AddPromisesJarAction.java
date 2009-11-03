package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import edu.cmu.cs.fluid.dc.Nature;

public class AddPromisesJarAction implements IViewActionDelegate {
	private IProject project;
	
	public void init(IViewPart view) {
		//Nothing to do
	}

	public void selectionChanged(IAction action, ISelection selection) {
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

	public void run(IAction action) {
		if (project != null) {
			Nature.finishProjectSetup(project, true);
		}
	}
}
