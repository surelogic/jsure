package com.surelogic.jsure.core.scripting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;

import com.surelogic.common.XUtil;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.core.java.JavaBuild;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.logging.IErrorListener;
import com.surelogic.jsure.core.driver.JavacDriver;

/**
 * Reads the script line by line
 * 
 * @author Edwin
 */
public class ScriptReader extends AbstractSLJob implements ICommandContext {	
  protected final Commands commands = new Commands();
  /**
   * Build after every command
   */
  boolean autoBuild = false;
  boolean changed   = false;
  boolean buildNow  = false;
  final Map<String,Object> args = new HashMap<String, Object>();  
  protected final List<IJavaProject> projects;
  final Set<IJavaProject> active = new HashSet<IJavaProject>(); // Used to simulate when only some are analyzed
  final boolean runAsynchronously;
  
  @Override
  public Object getArgument(String key) {
	  return args.get(key);
  }
  
  public ScriptReader(List<IJavaProject> p, boolean async) {
	super("Script Reader");
	projects = p;
	runAsynchronously = async;
	findActiveProjects(p);
	
	// Commands used for compatibility with old JSure model
	commands.put("addNature", new AbstractProjectCommand() {		
		@Override
		protected boolean execute(ICommandContext context, IProject p)
				throws Exception {
			for(IJavaProject jp : projects) {
				if (jp.getProject().equals(p)) {
					active.add(jp);
					break;
				}
			}
			return true;
		}
	});
	commands.put("removeNature", new AbstractProjectCommand() {		
		@Override
		protected boolean execute(ICommandContext context, IProject p)
				throws Exception {
			for(IJavaProject jp : projects) {
				if (jp.getProject().equals(p)) {
					active.remove(jp);
					break;
				}
			}
			return true;
		}
	});
	
	// Setup commands to change the state of autoBuild
    commands.put("set", new AbstractCommand() {
      @Override
      public boolean execute(ICommandContext context, String... contents) throws Exception {
    	boolean building = false;
        if (ScriptCommands.AUTO_BUILD.equals(contents[1])) {
          if (changed) {
        	  changed = false;
        	  building = true;
        	  build();
          }
          autoBuild = true;
        }
        return building;
      }  
    });
    commands.put("unset", new AbstractCommand() {
      @Override
      public boolean execute(ICommandContext context, String... contents) throws Exception {
        if (ScriptCommands.AUTO_BUILD.equals(contents[1])) {
          autoBuild = false;
        }
        return false;
      }  
    });
    commands.put(ScriptCommands.RUN_JSURE, new AbstractCommand() {
        @Override
        public boolean execute(ICommandContext context, String... contents) throws Exception {
        	if (contents.length > 0) {
        		// Lookup projects
        		List<IJavaProject> projs = new ArrayList<IJavaProject>(contents.length);
        		boolean first = true;
        		for(String p : contents) {
        			if (first) {
        				// Skip the first (command)
        				first = false;
        				continue;
        			}
        			IJavaProject proj = JDTUtility.getJavaProject(p);
        			if (proj == null) {
        				throw new IllegalArgumentException("Unknown project: "+proj);
        			}
        			projs.add(proj);
        		}
        		JavaBuild.analyze(JavacDriver.getInstance(), projs, IErrorListener.throwListener);
        	} else {
        		build();
        	}
            return false;
        }        
    });
	commands.put(ScriptCommands.EXPECT_BUILD, new SetFileArg(ScriptCommands.EXPECT_BUILD));
	commands.put(ScriptCommands.EXPECT_ANALYSIS, new SetFileArg(ScriptCommands.EXPECT_ANALYSIS));
  }
  
  private void findActiveProjects(List<IJavaProject> p) {
	  active.clear();
	  for(IJavaProject jp : p) {
		final String projectPath = jp.getProject().getLocation().toOSString();
		final File proj = new File(projectPath);
		final File dotProj = new File(proj, ".project");
		// Check if the file contains dcNature
		if (contains(dotProj, ".jdt.core.java"/*"dcNature"*/)) {
			active.add(jp);
		}
	  }	
  }

  private boolean contains(File f, String keyword) {
	  //System.out.println("Looking at "+f);
	  try {
		  FileReader fr = new FileReader(f);
		  BufferedReader br = new BufferedReader(fr);
		  String line;
		  try {
			  while ((line = br.readLine()) != null) {
				  //System.out.println("Scanning: "+line);
				  if (line.contains(keyword)) {
					  //System.out.println("Found "+keyword);
					  return true;
				  }
			  }
		  } finally {
			  br.close();
		  }
	  } catch(IOException e) {
		  // Ignore
	  }
	  return false;
  }

  public static void main(String[] args) throws Exception {
    ScriptReader r = new ScriptReader(null, false);
    try {
      r.executeScript(
          "set autobuild\n"+
          "unset autosave\n"+
          "set compiler 1.5\n"+
          "openProject foo\n"+
          "touchFile foo/Foo.java\n"+
          "saveFile foo/Foo.java\n"+
          "patchFile foo/Bar.java patch.txt\n"+
          "saveAllFiles\n"+
          "openProject bar\n"+
          "cleanProject foo, bar\n"+
          "closeProject foo\n"+
          "closeProject bar");
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
  public boolean executeScript(String script) throws Throwable {
    return execute(new StringReader(script));
  }
  
  public boolean execute(String name) throws Throwable {
    return execute(new File(name));
  }
  
  public boolean execute(File f) throws Throwable {
    return execute(f, f.getParentFile());
  }
  
  public boolean execute(File scriptFile, File workingDir) throws Throwable {
  final Reader r = new InputStreamReader(new FileInputStream(scriptFile));  
    args.put(SCRIPT_DIR, workingDir);
    return execute(r);
  }
  
  private static final String EXPECT_BUILD = ScriptCommands.EXPECT_BUILD+' ';
  static final String SCRIPT_DIR = "script.directory";

  /**
   * Fields only used by execute() below
   */
  BufferedReader br;
  String lastLine = null;
  String line;
  boolean resultsOk = true;
  
  public boolean execute(Reader r) throws Throwable {
	init();
	  
	br = new BufferedReader(r);  
	lastLine = null;
	resultsOk = true;
	/*
    while (resultsOk && (line = br.readLine()) != null) {    	
    	executeOneIteration();
    }
    if (resultsOk && lastLine != null) {
    	resultsOk = executeLine(lastLine) && resultsOk;
    }*/
	final SLStatus s = run(null);
	if (s.getException() != null) {
		throw s.getException();
	}
    return resultsOk;
  }

  /**
   * Assuming that things were already initialized
   * 
   * TODO how to tell if it stopped?
   */
  @Override
  public SLStatus run(SLProgressMonitor monitor) {
	  try {
		  while (resultsOk && (line = br.readLine()) != null) {    	
			  boolean building = executeOneIteration();
			  if (runAsynchronously && building) {
				  // Reschedule the rest of the loop
				  //JavacDriver.getInstance().waitForJSureBuild();
				  
				  System.out.println("Rescheduling script reader again");
				  EclipseUtility.toEclipseJob(this).schedule();
				  return SLStatus.OK_STATUS;
			  }
		  }
		  if (resultsOk && lastLine != null) {
			  // Doesn't matter if I started a build as a result
			  executeLine(lastLine);
		  }
		  finish();
	  } catch (Throwable e) {
		  return SLStatus.createErrorStatus(e);
	  }	  
	  return SLStatus.OK_STATUS;
  }
  
  /**
   * @return true if the command started a build
   */  
  private boolean executeOneIteration() throws Throwable {
	  if (line.length() == 0) {
		  return false;
	  }
	  line = line.trim();
	  if (line.startsWith("#")) {
		  System.out.println("ScriptReader: ignoring "+line);
		  return false;
	  }
	  if (line.startsWith(ScriptCommands.GO_FIRST)) {
		  // Handle these first, since it's out of order in the script
		  return executeLine(line.substring(1));		      	  
	  } 
	  else if (line.startsWith(EXPECT_BUILD)) { // Kept for compatibility
		  // Handle this first, since it's out of order in the script
		  return executeLine(line.substring(0));		     	  
	  } 
	  boolean built = false;
	  if (lastLine != null) {
		  built = executeLine(lastLine);
		  if (!resultsOk) {
			  throw new IllegalStateException("Failed on: "+lastLine);
		  } else {
			  System.out.println("OK: "+lastLine);
		  }
	  }      
	  lastLine = line;
	  return built;
  }
  
  /**
   * Sets resultsOk directly
   * @return true if the command started a build
   */  
  private boolean executeLine(String line) throws Throwable {
	  System.out.println("ScriptReader: "+line);
	  final String[] tokens = Util.collectTokens(line, " ,\n");
	  if (tokens.length == 0) {
		  return false; // Nothing to do
	  }	  
	  final ICommand command = commands.get(tokens[0]);
	  try {
		  final boolean justChanged = command.execute(this, tokens);
		  changed = changed || justChanged;
		  /*
      if (justChanged) {
    	System.out.println("ScriptReader: just changed");
      }
		   */
		  if (!command.succeeded()) {
			  throw new IllegalStateException("Failed on line: "+line);
		  }
		  
		  if (buildNow || changed && autoBuild) {
			  if (buildNow) {
				  System.out.println("ScriptReader: building now");
			  } else {
				  System.out.println("ScriptReader: auto-building due to a change");
			  }
			  changed  = false;
			  buildNow = false;
			  build();
			  return true;
		  }
		  return false;
	  } catch (Throwable e) {
		  System.out.println("Got exception on line: "+line);
		  e.printStackTrace(System.out);
		  command.succeeded();
		  resultsOk = false;
		  throw e;
	  }
  }
  
  protected void init() throws CoreException {
	  final IWorkspace workspace = ResourcesPlugin.getWorkspace();
	  final IWorkspaceDescription description = workspace.getDescription();
	  if (description.isAutoBuilding()) {
		  description.setAutoBuilding(false);
		  workspace.setDescription(description);
	  }
	  build(IncrementalProjectBuilder.CLEAN_BUILD);
  }

  protected void finish() throws CoreException {
	  // Nothing to do yet
  }
  
  private void build() throws CoreException {	  
	  //build(IncrementalProjectBuilder.CLEAN_BUILD); //OK
	  //build(IncrementalProjectBuilder.FULL_BUILD); //NO
	  build(IncrementalProjectBuilder.INCREMENTAL_BUILD); //NO
	  //build(IncrementalProjectBuilder.AUTO_BUILD); //NO?
	  /*
	  try {
		  Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
	  } catch (OperationCanceledException e) {
		  e.printStackTrace();
	  } catch (InterruptedException e) {
		  e.printStackTrace();
	  }
	  */
  }
  
  private void build(int kind) throws CoreException {
	if (projects != null && !active.isEmpty()) {
		try {
			System.out.println("Sleeping to let the file system sync ...");
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
		System.out.println("Building.");

		
		System.out.println("Analyzing.");
		JavaBuild.analyze(JavacDriver.getInstance(), new ArrayList<IJavaProject>(active), IErrorListener.throwListener);
		System.out.println("FINISHED build for: ");
		for(IJavaProject p : active) {
			System.out.println("\t"+p.getElementName());
		}
	} else {
		System.out.println("build workspace");
		ResourcesPlugin.getWorkspace().build(kind, null);
		throw new UnsupportedOperationException("No longer doing builds with an Eclipse Builder");
	}
  }
  
  public static void waitForBuild(int kind) throws CoreException {
      if (XUtil.testing) {
          // No check in JavacBuild, so no need to try to build
          return;
      }
	  ResourcesPlugin.getWorkspace().build(kind, null);
	  IJobManager jobMan = Job.getJobManager();
	  Job[] build = jobMan.find(ResourcesPlugin.FAMILY_AUTO_BUILD); 
	  if (build.length == 1) {
		  try {
			  build[0].join();
		  } catch (InterruptedException e) {
			  // ignore
		  }
	  }
  }
  
  class SetFileArg extends AbstractCommand {
	private final String key;
	SetFileArg(String key) {
		this.key = key;
	}
    @Override
	public boolean execute(ICommandContext context, String... contents)
			throws Exception {
		final File expected = resolveFile(context, contents[1]);
		if(expected != null && expected.exists()) {
			System.out.println("Ignoring "+key+" due to changes in how JSure runs");
			//JavacDriver.getInstance().setArg(key, expected);
		} else {
			throw new FileNotFoundException(contents[1] + " does not exist"); 
		}
		return false;
	}
  }
}
