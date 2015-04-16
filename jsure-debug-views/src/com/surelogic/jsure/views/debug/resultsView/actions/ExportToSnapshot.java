package com.surelogic.jsure.views.debug.resultsView.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.core.resources.IFile;
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
import com.surelogic.common.core.scripting.ScriptCommands;
import com.surelogic.common.java.JavaProject;
import com.surelogic.common.java.JavaProjectSet;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.common.xml.XmlCreator;
import com.surelogic.javac.Javac;
import com.surelogic.java.persistence.JSureScanInfo;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.java.xml.JSureAnalysisXMLConstants;

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
        JavaProjectSet<? extends JavaProject> projs = scan.getProjects();
        outer: for (final JavaProject current : projs) {
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
      final boolean overwrite = MessageDialog.openQuestion(shell, "Folder Exists", "Oracle folder \"" + oracleName
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
    createAnalysisSettings(resultsBelongTo, scan);
  }

  private void createAnalysisSettings(final IProject resultsBelongTo, final JSureScanInfo scan) {
	final IFile analysisSettings = resultsBelongTo.getFile(ScriptCommands.ANALYSIS_SETTINGS);
    boolean createSettings = true;
    if (analysisSettings.exists()) {
        createSettings = MessageDialog.openQuestion(shell, "File Exists", ScriptCommands.ANALYSIS_SETTINGS
                + " already exists.  Overwrite?");
    }
    if (createSettings) {
    	Properties props = new Properties();
    	try {
    		props.load(new FileReader(new File(scan.getJSureRun().getDir(), Javac.JAVAC_PROPS)));

    		final SettingsCreator c = new SettingsCreator(new FileOutputStream(analysisSettings.getLocation().toFile()));			
    		c.create(props);
    	} catch (IOException e) {
    		SLLogger.getLogger().log(Level.WARNING, "Error creating " + analysisSettings.getLocation(), e);
    		MessageDialog.openError(shell, "Error creating analysis settings", e.getClass().getSimpleName()+": "+e.getMessage());
    	}
    	
        try {
        	analysisSettings.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (final CoreException e) {
        	SLLogger.getLogger().log(Level.WARNING, "Error refreshing " + analysisSettings.getLocation(), e);
        }    
    }
  }
  
  static class SettingsCreator extends XmlCreator implements JSureAnalysisXMLConstants {
	SettingsCreator(OutputStream out) throws IOException {
		super(out);
	}

	void create(Properties props) {
		b.start(SF_PREFS);
		Builder pb = b.nest(SF_INCLUDED_ANALYSIS_MODULES);
		final Set<String> excluded = new HashSet<String>();
		for(final Map.Entry<Object,Object> e : props.entrySet()) {
			final String key = e.getKey().toString();
			if (key.startsWith(IDEPreferences.ANALYSIS_ACTIVE_PREFIX)) {
				final String id = key.substring(IDEPreferences.ANALYSIS_ACTIVE_PREFIX.length());
				if ("true".equals(e.getValue().toString())) {
					makeId(pb, id);
				} else {
					excluded.add(id);
				}
			}
		}
		pb.end();
		pb = b.nest(SF_EXCLUDED_ANALYSIS_MODULES);
		for(String id : excluded) {
			makeId(pb, id);
		}
		pb.end();
		b.end();
	}

	private void makeId(Builder pb, String id) {
		Builder ib = pb.nest(SF_ID);
		ib.endWithContents(id);
	}	  
  }
}
