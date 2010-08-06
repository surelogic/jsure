package edu.cmu.cs.fluid.dc;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;

import com.surelogic.jsure.client.eclipse.analysis.*;

/**
 * Implements a context menu action for IProject and IJavaProject that removes the
 * Fluid nature from a set of projects.
 */
public class RemoveFluidNatureAction extends SelectedProjectsAction {
  @Override
  protected boolean doRun(IProject project) {
    try {
      if (project != null && Nature.hasNature(project)) {
    	  Nature.removeNatureFromProject(project);
    	  JavacDriver.getInstance().recordProjectAction(ScriptCommands.REMOVE_NATURE, project);
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
