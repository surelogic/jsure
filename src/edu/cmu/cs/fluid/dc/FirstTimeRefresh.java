package edu.cmu.cs.fluid.dc;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class FirstTimeRefresh extends FirstTimeJob {
  public FirstTimeRefresh(IProject project) {
    super("JSure Assurance Tool", project);
  }

  @Override
  protected void doJob(IProgressMonitor monitor) throws CoreException {
    m_project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    
	/*
	 * Schedule Eclipse to run our initial analysis (an AUTO_BUILD of
	 * project). We need this because we don't (currently) persist our
	 * results. Note that this job should run after the one above (I belive
	 * this is what is occurring in Eclipse).
	 */	
	new FirstTimeAnalysis(m_project).schedule();
  }
}
