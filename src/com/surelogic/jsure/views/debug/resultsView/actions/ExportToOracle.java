package com.surelogic.jsure.views.debug.resultsView.actions;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.dc.Nature;
import edu.cmu.cs.fluid.dcf.views.coe.XMLReport;

public class ExportToOracle implements IViewActionDelegate {
  private final SimpleDateFormat dateFormat =
    new SimpleDateFormat("'oracle'yyyyMMdd'.zip'");
  private IViewPart currentView = null;
  private Shell shell = null;
  
  
  public void init(final IViewPart view) {
    currentView = view;
    shell = view.getViewSite().getShell();
  }

  public void selectionChanged(
      final IAction action, final ISelection selection) {
    // We don't care about selections
  }

  public void run(final IAction action) {
    /* Find the project that the results belong to.  This is 
     * sleazy and probably won't work in the future.
     */
    IProject resultsBelongTo = null;
    final IProject[] projects =
      ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for (final IProject current : projects) {
      if (current.isOpen() && Nature.hasNature(current)) {
        resultsBelongTo = current;
        break;
      }
    }
    if (resultsBelongTo == null) {
      MessageDialog.openError(shell, "Couldn't Find Project", 
          "Couldn't determine which project the results belong to.");
      return;
    }
   
    final String oracleName = dateFormat.format(new Date());
    final IFile oracleFile = resultsBelongTo.getFile(oracleName);
    
    if (oracleFile.exists()) {
      final boolean overwrite =
        MessageDialog.openQuestion(shell, "File Exists",
            "Oracle file \"" + oracleName + "\" already exists.  Overwrite?");
      if (!overwrite) {
        return;
      }
    }
    
    FileOutputStream zipFile;
    try {
      zipFile = new FileOutputStream(oracleFile.getLocation().toOSString());
    } catch (final FileNotFoundException e) {
      SLLogger.getLogger().log(Level.WARNING,
          "Unable to create oracle file " + oracleFile.getLocation(), e);
      MessageDialog.openError(shell, "Error exporting results",
          "Unable to create oracle file");
      return;
    }
    
    XMLReport.exportResultsWithSource(
        zipFile, (TreeViewer) currentView.getAdapter(TreeViewer.class));
    
    /* Refresh the worksapce to pick up the new file.  There has to be
     * better way to create the IFile directly, but I cannot find one.
     */
    try {
      oracleFile.refreshLocal(1, null);
    } catch (final CoreException e) {
      SLLogger.getLogger().log(Level.WARNING,
          "Error refreshing " + oracleFile.getLocation(), e);
      MessageDialog.openError(shell, "Error exporting results",
          "Unable to refresh oracle file");
    }
  }
}
