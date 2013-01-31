package com.surelogic.jsure.views.debug.resultsView.actions;

import java.util.logging.Level;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com.surelogic.common.FileUtility;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.javac.JavacProject;
import com.surelogic.javac.Projects;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public class ExportToSnapshot implements IViewActionDelegate {
  // private IViewPart currentView = null;
  private Shell shell = null;

  @Override
  public void init(final IViewPart view) {
    // currentView = view;
    shell = view.getViewSite().getShell();
  }

  @Override
  public void selectionChanged(final IAction action, final ISelection selection) {
    // We don't care about selections
  }

  @Override
  public void run(final IAction action) {
    /*
     * Find the project that the results belong to. 
     */
    IProject resultsBelongTo = null;

    final JSureScanInfo scan = JSureDataDirHub.getInstance().getCurrentScanInfo();
    if (scan != null) {
      try {
        Projects projs = scan.getProjects();
        outer: for (final JavacProject current : projs) {
        	if (!current.isAsBinary()) {
        		IProject p = EclipseUtility.getProject(current.getName());
        		if (p != null && p.isOpen()) {
        			resultsBelongTo = p;
        			break outer;
        		}
        	}          
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (resultsBelongTo == null) {
      MessageDialog.openError(shell, "Couldn't Find Project", "Couldn't determine which project the results belong to.");
      return;
    }

    final String oracleName = SLUtility.getScanDirectoryName(RegressionUtility.ORACLE_SCAN_DIR_PREFIX, scan.getProjects()
        .multiProject(), scan.getJSureRun().getTimeOfScan());
    final IFolder oracleFile = resultsBelongTo.getFolder(oracleName);

    if (oracleFile.exists()) {
      final boolean overwrite = MessageDialog.openQuestion(shell, "File Exists", "Oracle file \"" + oracleName
          + "\" already exists.  Overwrite?");
      if (!overwrite) {
        return;
      }
    }

    FileUtility.recursiveCopy(scan.getJSureRun().getDir(), oracleFile.getLocation().toFile());

    /*
     * Refresh the worksapce to pick up the new file. There has to be better way
     * to create the IFile directly, but I cannot find one.
     */
    try {
      oracleFile.refreshLocal(IResource.DEPTH_INFINITE, null);
      MessageDialog.openInformation(shell, "Oracle exported", "The oracle should now appear in "+oracleFile.getFullPath());
    } catch (final CoreException e) {
      SLLogger.getLogger().log(Level.WARNING, "Error refreshing " + oracleFile.getLocation(), e);
      MessageDialog.openError(shell, "Error exporting results", "Unable to refresh oracle file");
    }
  }
}
