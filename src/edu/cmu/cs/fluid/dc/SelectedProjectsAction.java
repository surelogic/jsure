package edu.cmu.cs.fluid.dc;

import java.util.*;
import java.util.logging.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;

import com.surelogic.common.logging.SLLogger;

public abstract class SelectedProjectsAction implements IViewActionDelegate, IObjectActionDelegate {
  protected static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.dc");
  
  @SuppressWarnings("unchecked")
  private List selectedProjects = null;

  public final void setActivePart(final IAction action, final IWorkbenchPart targetPart) {
    // do nothing
  }
  
  public final void init(final IViewPart view) {
    // do nothing    
  }

  public final void selectionChanged(final IAction action, final ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      selectedProjects = ((IStructuredSelection) selection).toList();
    } else {
      selectedProjects = null;
    }
  }

  public final void run(final IAction action) {
    if (selectedProjects != null) {
      for (Object current : selectedProjects) {
        if (current != null) {
          final IProject project = (IProject) ((IAdaptable) current).getAdapter(IProject.class);
          if (project != null) {
        	  doRun(project);
          }
        }
      }
    }
    finishRun();
  }
  
  protected final Iterable<IProject> getSelectedProjects() {
	  List<IProject> projects = new ArrayList<IProject>();
	  for (Object current : selectedProjects) {
		  if (current != null) {
			  final IProject p = (IProject) ((IAdaptable) current).getAdapter(IProject.class);
			  projects.add(p);
		  }
	  }
	  return projects;
  }
  
  /**
   * @return true if operation was done successfully
   */
  protected abstract boolean doRun(IProject current);
  
  protected void finishRun() {
	  // Nothing to do
  }
}
