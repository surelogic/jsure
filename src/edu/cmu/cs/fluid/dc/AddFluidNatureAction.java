package edu.cmu.cs.fluid.dc;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Implements a context menu action for IProject and IJavaProject that sets the
 * Fluid nature for a set of project.
 */
public class AddFluidNatureAction extends SelectedProjectsAction {
  @Override
  protected boolean doRun(Object current) {
    final IProject project = (IProject) ((IAdaptable) current).getAdapter(IProject.class);
    try {
      if (project != null && !Nature.hasNature(project)) { 
    	  Nature.addNatureToProject(project);
    	  return true;
      }
    } catch (CoreException e) {
    	Majordomo.logError("Error While Adding JSure Nature", 
    			"Unable to add JSure nature from Java project "
          + project.getName(), e);
    }
    return false;
  }
}
