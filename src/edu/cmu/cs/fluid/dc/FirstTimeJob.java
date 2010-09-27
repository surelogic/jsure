package edu.cmu.cs.fluid.dc;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.surelogic.common.XUtil;
import com.surelogic.common.logging.SLLogger;

/**
 * Subclasses will be run upon startup of this plugin
 */
public abstract class FirstTimeJob extends Job {

  protected static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.dc");  
  
  protected final IProject m_project;
  
  public FirstTimeJob(String name, IProject project) {
    super(name);
    m_project = project;
  }

  /**
   * Perform initial program analysis on #project (a Java project).
   * 
   * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public IStatus run(IProgressMonitor monitor) {
    final String name = this.getClass().getSimpleName();
    
    if (m_project == null) {
      LOG.log(Level.SEVERE, "project is null...should be impossible");
      return new Status(IStatus.ERROR, Plugin.DOUBLE_CHECKER_PLUGIN_ID,
          IStatus.OK,
          "project passed to double-checker to do "+name+" is null",
          null);
    }
    try {
      doJob(monitor);
    } catch (OperationCanceledException e) {
      LOG.log(Level.INFO, name+" cancelled", e);
      return Status.CANCEL_STATUS;
    } catch (CoreException e) {
      if (XUtil.testing) {
    	  return e.getStatus();
      }
      LOG.log(Level.SEVERE, name+" failed", e);
      return new Status(IStatus.ERROR, Plugin.DOUBLE_CHECKER_PLUGIN_ID,
          IStatus.OK,
          name+" encountered a CoreException examining "
              + m_project.getName(), e);
    }
    return Status.OK_STATUS;
  }
  
  protected abstract void doJob(IProgressMonitor monitor) throws CoreException;
}
