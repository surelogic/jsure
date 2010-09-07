package com.surelogic.jsure.tests;

import java.io.*;
import java.util.*;

import junit.framework.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.preference.IPreferenceStore;

import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.jsure.client.eclipse.analysis.*;
import com.surelogic.test.*;
import com.surelogic.test.scripting.*;
import com.surelogic.test.xml.JUnitXMLOutput;

import edu.cmu.cs.fluid.analysis.util.ConsistencyListener;
import edu.cmu.cs.fluid.dc.*;
import edu.cmu.cs.fluid.dc.Plugin;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.eclipse.logging.EclipseLogHandler;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.logging.XMLLogDiff;
import edu.cmu.cs.fluid.sea.xml.*;

/**
 * Assumes that the workspace is already setup:
 * -- The appropriate projects have the Fluid nature
 * -- 
 */
public class RegressionTest extends TestCase implements IAnalysisListener {
  class InitRunnable implements Runnable {
    boolean run = false;
    
    public void run() {
      Eclipse.getDefault().addTestOutputFactory(JUnitXMLOutput.factory);
      System.out.println("Added JUnitXMLOutput.factory");
      run = true;
    }

    public void ensureInit() {
      if (!run) {
        run();
      }
    }
  }
  
  /**
   * Returns the JUnit test suite that implements the <b>TestTest</b>
   * definition.
   */

  /**
   * @see junit.framework.TestCase#setUp()
   */
  @Override
  protected void setUp() throws Exception {
    InitRunnable r = new InitRunnable();
    Eclipse.initialize(r);
    r.ensureInit();
    
    initializeWorkspace();
  }

  private void initializeWorkspace() {   
    final String testModule   = System.getProperty("test.module");
    final String extraModules = System.getProperty("extra.test.modules");
    System.out.println(testModule);
    System.out.println(extraModules);
    if (testModule != null) {      
      importProject(testModule);
      
      if (extraModules != null && !extraModules.startsWith("${")) {
    	final StringTokenizer st = new StringTokenizer(",");
    	while (st.hasMoreTokens()) {
    	  final String extra = st.nextToken().trim();
    	  importProject(extra);
    	}
      }
    }
  }

  private void importProject(final String projectDir) {	
    final File file                 = new File(projectDir);
    importProject(file);
  }
    
  /**
   * Change to check for a .project file
   * If not present, try to import immediate subdirectories
   */
  private void importProject(final File file) {	
    // check for a .project file
    if (!new File(file, ".project").exists()) {
    	// Not present, so assume it to be a multi-project container, 
    	// and try to import immediate subdirectories
    	for(File f : file.listFiles()) {
    		if (f.isDirectory()) {
    			importProject(f);
    		}
    	}
    	return;
    }
    final String project            = file.getName();
    final IWorkspace workspace      = ResourcesPlugin.getWorkspace();
    IProjectDescription description = setProjectName(workspace, findDotProjectFile(file));
    final IWorkspaceRoot wsRoot     = workspace.getRoot();
    final IProject proj;
    if (description != null) {
      proj = wsRoot.getProject(description.getName());
    } else {
      proj = wsRoot.getProject(project);
    }
    if (!proj.exists()) {
      try {
        if (description == null) {
          System.out.println("creating new description: " + null);
          description = workspace.newProjectDescription(project);
          description.setLocation(new Path(null));
        }

        System.out.println("creating project: ");
        proj.create(description, null);

      } catch (CoreException e) {
        e.printStackTrace();
      }
    }
    // Project should exist now
    if (proj.exists()) {
    	final IResource res = proj.findMember(".projectStatus");
    	if (res == null) {
    		// do the default thing of opening the project
            try {
            	System.out.println("opening project: ");
            	proj.open(null);
            } catch (CoreException e) {
            	e.printStackTrace();
            }
    	}
    }    
  }

  /**
   * Return a .project file from the specified location. If there isn't one
   * return null.
   */
  private File findDotProjectFile(File directory) {
    System.out.println("projectFile(File): " + directory.getAbsolutePath());
    File ret = null;
    if (directory.isDirectory()) {
      File[] files = directory.listFiles(this.projectFilter);
      if (files != null && files.length == 1) {
        ret = files[0];
      }
    }
    if (ret != null) {
    	System.out.println("Found .project File: " + ret.getAbsolutePath());
    }
    return ret;
  }
  
  private FileFilter projectFilter = new FileFilter() {
    // Only accept those files that are .project
    public boolean accept(File pathName) {
      return pathName.getName().equals(
          IProjectDescription.DESCRIPTION_FILE_NAME);
    }
  };
  
  /**
   * Set the project name using either the name of the parent of the file or the
   * name entry in the xml for the file
   */
  private IProjectDescription setProjectName(IWorkspace workspace, File projectFile) {
    System.out.println("setProjectName(File) - ProjectFile:  - projectFile="
        + projectFile.getAbsolutePath());

    // If there is no file or the user has already specified forget it
    if (projectFile != null) {
      IPath path = new Path(projectFile.getPath());
      try {
        return workspace.loadProjectDescription(path);
      } catch (CoreException exception) {
        // no good couldn't get the name
      }
    }
    return null;
  }
  
  /**
   * @see junit.framework.TestCase#tearDown()
   */
  @Override
  protected void tearDown() throws Exception {
    // nothing to do
  }

  void closeAllProjects(IWorkspaceRoot root) throws CoreException {
    for (IProject p : root.getProjects()) { 
      if (p.isOpen()) {
        p.close(null); 
      } 
    }
  }
  
  void createAndOpenProject(IWorkspaceRoot root, String name) throws CoreException {
    IProject project = root.getProject(name);
    if (!project.exists()) {
      project.create(null);
      System.out.println("Creating a project: " + name);
    } else {
      System.out.println("Already created:    " + name);
    }

    if (!Nature.hasNature(project)) {
      System.out.println("Adding nature to project: " + name);
      Nature.addNatureToProject(project);
    }

    // Open and then stop processing
    if (!project.isOpen()) {
      project.open(null);
      System.out.println("Opening a project:  " + name);
    } else {
      System.out.println("Already opened:     " + name);
    }
  }
  
  public void testMajordomo() throws Throwable{
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    String workspacePath = root.getLocation().toOSString();
    File workspaceFile = new File(workspacePath);
    System.out.println("workspace = " + workspacePath);

    /* 
    for (String name : workspaceFile.list()) {
      if (name.startsWith(".") || name.equals("promises")) {
        continue;
      }
      File f = new File(name);
      if (!f.isDirectory()) {
        continue;
      }
      createAndOpenProject(root, name);
    }
    */
    
    IProject[] projects = root.getProjects();
    /*
    for (IProject project : projects) {
      // check for the .settings folder
      URI location = project.getLocationURI();
      File settings = new File(location.toString(), ".settings");

      // If there are no project-specific settings, set some
      if (!settings.exists()) {
        // Set the compiler compliance to Java 1.5
        // This sets it for the whole system I believe...need to make this on
        // a
        // per-project basis in case we want multi-project tests
        Hashtable options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
            JavaCore.VERSION_1_5);

        JavaCore.setOptions(options);
      }
    }
    */
    
    // Just used to get events to see how the analysis is running
    NotificationHub.addAnalysisListener(this);

    // Assumes that there's only one project getting analyzed
    IProject project = null;    
    for (int i = 0; i < projects.length; i++) {      
      final IProject p = projects[i];
      if (p.getName().equals("promises")) {
        continue;
      }
      if (p.isOpen()) {
    	  File script = findScript(p);
    	  if (script != null || Nature.hasNature(p)) {
    		  assertNull("More than one project to analyze!?!", project);
    		  project = p;
    	  }
      }      
    }

    
    if (project == null) {
      fail("No project");
      return;
    } 
    output = IDE.getInstance().makeLog(project.getName());
    try {
      runAnalysis(workspaceFile, project);
    } catch(AssertionFailedError e) {
      output.close();
      throw e; // pass-through
    } catch(Throwable ex) {
      ex.printStackTrace();
      output.reportError(currentTest, ex);
      output.close();    
      throw ex;
    }
    output.close();
  }
  
  private ITest start(final String tag) {
    System.out.println("RegressionTest: "+tag);
    ITest test = new ITest() {
      public String getClassName() {
        return tag; 
      }
      public IRNode getNode() {
        return null;
      }   
      @Override
      public String toString() {
    	  return "RegressionTest "+tag;
      }
    };
    return output.reportStart(test);
  }
  
  private void end(String msg) {
    output.reportSuccess(currentTest, msg);
    currentTest = null;
  }
  
  private void endError(Throwable t) {
    output.reportError(currentTest, t);
    currentTest = null;
  }
  
  private ITestOutput output = null;
  private ITest currentTest = null;
  
  private File findScript(final IProject project) {
	  final String projectPath  = project.getLocation().toOSString();
	  File script = new File(projectPath + File.separatorChar + ScriptCommands.NAME);
	  if (script.exists() && script.isFile()) {
		  return script;
	  }
	  return null;
  }
  
  private void runAnalysis(final File workspaceFile, final IProject project) throws Throwable {
    final String projectPath  = project.getLocation().toOSString();

    currentTest = start("Check for analysis settings");
    printActivatedAnalyses();
    
    final File analyses = new File(projectPath + File.separatorChar
        + ScriptCommands.ANALYSIS_SETTINGS);
    if (analyses.exists() && analyses.isFile()) {
      System.out.println("Found project-specific analysis settings.");
      IPreferenceStore store = JSureAnalysisXMLReader.readStateFrom(analyses);
      Plugin.getDefault().initAnalyses(store);
      
      if (AnalysisDriver.useJavac) {
    	System.out.println("Configuring analyses from project-specific settings");
    	JavacEclipse.initialize();
      	((JavacEclipse) IDE.getInstance()).synchronizeAnalysisPrefs(store);
      }
    } else {
      System.out.println("No project-specific analysis settings.");
    }
    end("Done checking analysis settings");

    currentTest = start("Start logging to a file & refresh");
    final String logName = EclipseLogHandler.startFileLog(project.getName() + ".log");
    ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);

    printActivatedAnalyses();
    end("Done with refresh");

    // Force a build of the workspace
    // Does the analysis and updates the consistency proof
    currentTest = start("Build and analyze");
    ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.AUTO_BUILD, null);
    end("Done analyzing");
     
    final String projectName = project.getName();
    boolean resultsOk = true;
    
    // Check for script in the project to execute      
    File script = findScript(project);
    if (script != null) {
      currentTest = start("Run scripting");
      ScriptReader r = new ScriptReader(project);
      resultsOk = r.execute(script);
      end("Done scripting");
    }
      
    // Checks consistency of TestResults
    System.out.println("Updating consistency proof"); 
    ConsistencyListener.prototype.analysisCompleted();               
    
    assertNotNull(projectName);
    EclipseLogHandler.stopFileLog();
    System.out.println("log = " + logName);

    AnnotationRules.XML_LOG.close();

    //String resultsName = null;
    
    // Export the results from this run
    currentTest = start("Exporting results");
    try {      
      /* Old results
      File f = new File(workspaceFile, projectName + ".results.zip");
      FileOutputStream out = new FileOutputStream(f);
      System.out.println("Exporting results w/ source");
      XMLReport.exportResultsWithSource(out);
      out.close();
      resultsName = f.getAbsolutePath();
      assert (f.exists());

      System.out.println("results = " + resultsName);
      */
    	
      // Export new results XML
      // final File location = new File(workspaceFile, projectName + SeaSnapshot.SUFFIX);
      // SeaSummary.summarize(projectName, Sea.getDefault(), location);      
      new ExportResults().execute(ICommandContext.nullContext, ScriptCommands.EXPORT_RESULTS, projectName, projectName);
      end("Done exporting");
      
      currentTest = start("comparing results");
      System.out.println("Try to compare these results to the results oracle");    
      if (projectPath != null) {	  
    	resultsOk = compareResults(workspaceFile, projectPath, projectName, resultsOk);
    	/*
    	final String oracleName = getOracleName(projectPath, oracleFilter, "oracle.zip");
        System.out.println("Looking for " + oracleName);
        assert (new File(oracleName).exists());

        final Results results = Results.doDiff(oracleName, resultsName);
        assertNotNull(results);

        String diffsName = projectName + ".diffs.xml";
        results.generateXML(diffsName);
        System.out.println("diffs.xml = " + diffsName);
        System.out.println("Has children? " + results.root.hasChildren());
        if (!useNewSnapshotXML) {
        	resultsOk = !results.root.hasChildren();
        }
        */
        end("Done comparing");
      }
    } catch (FileNotFoundException ex) {
      System.out.println("Problem while creating results:");
      endError(ex);
    } catch (IOException ex) {
      System.out.println("Problem while closing results:");
      endError(ex);
    } catch (Throwable ex) {
      System.out.println("Problem while exporting/comparing results: "+ex+" -- "+ex.getMessage());
      ex.printStackTrace(System.out);
      endError(ex);
    }

    final boolean logOk;
    currentTest = start("comparing logs");
    System.out.println("Try to compare the log to the log oracle");
    if (projectPath != null) {
      final ITestOutput XML_LOG = IDE.getInstance().makeLog("EclipseLogHandler");
      final String oracleName = 
    	  RegressionUtility.getOracleName(projectPath, 
    			  RegressionUtility.logOracleFilter,
                                              "oracle.log.xml");
      final String logDiffsName = projectName + ".log.diffs.xml";
      assert (new File(oracleName).exists());
      try {
        System.out.println("Starting log diffs");
        int numDiffs = XMLLogDiff.diff(XML_LOG, oracleName, logName,
            logDiffsName);
        System.out.println("#diffs = " + numDiffs);
        logOk = (numDiffs == 0);
        System.out.println("log diffs = " + logDiffsName);
        end("Done comparing logs");
      } catch (Throwable e) {
        System.out.println("Problem while diffing the log: " + oracleName
            + ", " + logName + ", " + logDiffsName);
        endError(e);
        throw e;
      } 
      finally{
        XML_LOG.close();
      }
    } else {
      logOk = false;
    }
    assertTrue("results = "+resultsOk+", log = "+logOk, 
               resultsOk && logOk);
  }

  private boolean compareResults(final File workspaceFile,
		  final String projectPath, final String projectName, boolean resultsOk)
  throws Exception {
	  final File xmlLocation = SeaSummary.findSummary(projectPath);
	  if (!xmlLocation.exists()) {
		  return resultsOk;
	  }
	  String diffPath = new File(workspaceFile, 
			  projectName+RegressionUtility.JSURE_SNAPSHOT_DIFF_SUFFIX).getAbsolutePath(); 
	  CompareResults compare = new CompareResults();
	  //System.out.println("compare "+projectName+" "+xmlLocation.getAbsolutePath()+" "+diffPath);
	  compare.execute(ICommandContext.nullContext, "compare", projectName, 
			          xmlLocation.getAbsolutePath(), diffPath);
	  return resultsOk && compare.resultsOk;
  }

  private void printActivatedAnalyses() {
    for (String id : Plugin.getDefault().getIncludedExtensions()) {
      System.out.println("Activated: " + id);
    }
  }

  public synchronized void analysisCompleted() {
    System.out.println("Analysis completed");
  }

  public synchronized void analysisPostponed() {
    System.out.println("Analysis postponed");
  }

  public synchronized void analysisStarting() {
    System.out.println("Analysis starting");
  }
}
