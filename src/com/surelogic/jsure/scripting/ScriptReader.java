package com.surelogic.jsure.scripting;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import com.surelogic.common.eclipse.jobs.EclipseJob;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.jsure.client.eclipse.analysis.JavacDriver;
import com.surelogic.jsure.client.eclipse.analysis.ScriptCommands;

import edu.cmu.cs.fluid.dc.FirstTimeAnalysis;

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
  boolean autoBuild = true;
  boolean changed   = false;
  boolean buildNow  = false;
  final Map<String,Object> args = new HashMap<String, Object>();  
  final IProject project;
  final boolean runAsynchronously;
  
  public Object getArgument(String key) {
	  return args.get(key);
  }
  
  public ScriptReader(IProject p, boolean async) {
	super("Script Reader");
	project = p;
	runAsynchronously = async;
	  
	// Setup commands to change the state of autoBuild
    commands.put("set", new AbstractCommand() {
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
      public boolean execute(ICommandContext context, String... contents) throws Exception {
        if (ScriptCommands.AUTO_BUILD.equals(contents[1])) {
          autoBuild = false;
        }
        return false;
      }  
    });
	commands.put(ScriptCommands.EXPECT_BUILD, new SetFileArg(ScriptCommands.EXPECT_BUILD));
	commands.put(ScriptCommands.EXPECT_ANALYSIS, new SetFileArg(ScriptCommands.EXPECT_ANALYSIS));
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
    final Reader r = new InputStreamReader(new FileInputStream(f));  
    args.put(SCRIPT_DIR, f.getParentFile());
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
  public SLStatus run(SLProgressMonitor monitor) {
	  try {
		  while (resultsOk && (line = br.readLine()) != null) {    	
			  boolean building = executeOneIteration();
			  if (runAsynchronously && building) {
				  // Reschedule the rest of the loop
				  JavacDriver.getInstance().waitForJSureBuild();
				  
				  System.out.println("Rescheduling script reader again");
				  EclipseJob.getInstance().schedule(this);
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
		  e.printStackTrace();
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
	if (project != null) {
		try {
			System.out.println("Sleeping to let the file system sync ...");
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
		System.out.println("build FTA");
		final IStatus s = new FirstTimeAnalysis(project).run(null);
		if (!s.isOK()) {
			throw new CoreException(s);
		}
	} else {
		System.out.println("build workspace");
		ResourcesPlugin.getWorkspace().build(kind, null);
	}
  }
  
  class SetFileArg extends AbstractCommand {
	private final String key;
	SetFileArg(String key) {
		this.key = key;
	}
	public boolean execute(ICommandContext context, String... contents)
			throws Exception {
		final File expected = resolveFile(context, contents[1]);
		if(expected != null && expected.exists()) {
			JavacDriver.getInstance().setArg(key, expected);
		} else {
			throw new FileNotFoundException(contents[1] + " does not exist"); 
		}
		return false;
	}
  }
}
