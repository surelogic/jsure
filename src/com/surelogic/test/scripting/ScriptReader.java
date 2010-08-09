package com.surelogic.test.scripting;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
//import org.eclipse.core.runtime.jobs.Job;

/**
 * Reads the script line by line
 * 
 * @author Edwin
 */
public class ScriptReader implements ICommandContext {	
  private Commands commands = new Commands();
  /**
   * Build after every command
   */
  boolean autoBuild = true;
  boolean changed   = false;
  boolean buildNow  = false;
  
  public ScriptReader() {
	// Setup commands to change the state of autoBuild
    commands.put("set", new ICommand() {
      public boolean execute(ICommandContext context, String... contents) throws Exception {
        if ("autobuild".equals(contents[1])) {
          autoBuild = true;
        }
        return false;
      }  
    });
    commands.put("unset", new ICommand() {
      public boolean execute(ICommandContext context, String... contents) throws Exception {
        if ("autobuild".equals(contents[1])) {
          autoBuild = false;
        }
        return false;
      }  
    });
  }
  
  public static void main(String[] args) throws Exception {
    ScriptReader r = new ScriptReader();
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
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void executeScript(String script) throws Exception {
    execute(new StringReader(script));
  }
  
  public void execute(String name) throws Exception {
    execute(new File(name));
  }
  
  public void execute(File f) throws Exception {
    final Reader r = new InputStreamReader(new FileInputStream(f));  
    execute(r);
  }
  
  public void execute(Reader r) throws Exception {
	init();
	  
    final BufferedReader br = new BufferedReader(r);  
    String line;
    while ((line = br.readLine()) != null) {
      String[] tokens = Util.collectTokens(line, " ,\n");
      if (tokens.length == 0) {
        continue;
      }
      if (tokens[0].startsWith("#")) {
    	System.out.println("ScriptReader: ignoring "+line);
        continue;
      }
      System.out.println("ScriptReader: "+line);
      final boolean justChanged = commands.get(tokens[0]).execute(this, tokens);
      changed = changed || justChanged;
      /*
      if (justChanged) {
    	System.out.println("ScriptReader: just changed");
      }
      */
      if (buildNow || changed && autoBuild) {
    	if (buildNow) {
    	  System.out.println("ScriptReader: building now");
    	} else {
    	  System.out.println("ScriptReader: auto-building due to a change");
    	}
        changed  = false;
        buildNow = false;
        build();
      }
    }
  }

  private void init() throws CoreException {
	  final IWorkspace workspace = ResourcesPlugin.getWorkspace();
	  final IWorkspaceDescription description = workspace.getDescription();
	  if (description.isAutoBuilding()) {
		  description.setAutoBuilding(false);
		  workspace.setDescription(description);
	  }
	  build(IncrementalProjectBuilder.CLEAN_BUILD);
  }

  private void build() throws CoreException {	  
	  build(IncrementalProjectBuilder.CLEAN_BUILD); //OK
	  //build(IncrementalProjectBuilder.FULL_BUILD); //NO
	  //build(IncrementalProjectBuilder.INCREMENTAL_BUILD); //NO
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
	ResourcesPlugin.getWorkspace().build(kind, null);
	/*
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    
    for(IProject p : root.getProjects()) {
        // TODO what if these are run out of dependency order
    	// p.getReferencedProjects()
    	
    	// check if files changed    
    	p.build(kind, null);
    }
    */
  }
}
