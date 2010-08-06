package edu.cmu.cs.fluid.dc;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;

/**
 * Implements a context menu action for IProject and IJavaProject that causes those projects 
 * to be analyzed, if needed
 */
public final class AnalyzeNowAction extends SelectedProjectsAction {
  @Override
  protected boolean doRun(final IProject project) {
    if (project != null) {
    	new FirstTimeJob("On-demand JSure analysis of "+project.getName(), project) {
    		@Override
    		protected void doJob(IProgressMonitor monitor) throws CoreException {
    			Majordomo.analyze(project, monitor);
    		}        
    	}.schedule();
    }
    return false;
  }
}
