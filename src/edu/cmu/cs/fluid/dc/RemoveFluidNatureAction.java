package edu.cmu.cs.fluid.dc;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Implements a context menu action for IProject and IJavaProject that removes the
 * Fluid nature from a set of projects.
 */
public class RemoveFluidNatureAction extends SelectedProjectsAction {
  @Override
  protected boolean doRun(Object current) {
    final IProject project = (IProject) ((IAdaptable) current).getAdapter(IProject.class);
    try {
      if (project != null && Nature.hasNature(project)) {
    	  Nature.removeNatureFromProject(project);
    	  return true;
      }
    } catch (CoreException e) {
    	Majordomo.logError("Error While Removing JSure Nature", 
    			"Unable to remove JSure nature from Java project "
          + project.getName(), e);
    }
    return false;
  }
}
