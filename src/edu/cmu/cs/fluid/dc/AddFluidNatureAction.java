package edu.cmu.cs.fluid.dc;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;

import com.surelogic.jsure.client.eclipse.analysis.*;

/**
 * Implements a context menu action for IProject and IJavaProject that sets the
 * Fluid nature for a set of project.
 */
public class AddFluidNatureAction extends SelectedProjectsAction {
  @Override
  protected boolean doRun(IProject project) {
    try {
      if (project != null && !Nature.hasNature(project)) { 
    	  Nature.addNatureToProject(project);
    	  JavacDriver.getInstance().recordProjectAction(ScriptCommands.ADD_NATURE, project);
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
