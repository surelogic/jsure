package com.surelogic.test.scripting;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import com.surelogic.jsure.client.eclipse.analysis.JavacDriver;
import com.surelogic.jsure.client.eclipse.analysis.ScriptCommands;

import edu.cmu.cs.fluid.dc.FirstTimeAnalysis;

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
  final Map<String,Object> args = new HashMap<String, Object>();  
  final IProject project;
  
  public ScriptReader(IProject p) {
	project = p;
	  
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
	commands.put(ScriptCommands.EXPECT_BUILD, new ExpectBuild());
  }
  
  public static void main(String[] args) throws Exception {
    ScriptReader r = new ScriptReader(null);
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
  
  private static final String EXPECT_BUILD = ScriptCommands.EXPECT_BUILD+' ';
  
  public void execute(Reader r) throws Exception {
	init();
	  
    final BufferedReader br = new BufferedReader(r);  
    String lastLine = null;
    String line;
    while ((line = br.readLine()) != null) {
      if (line.length() == 0) {
        continue;
      }
      line = line.trim();
      if (line.startsWith("#")) {
    	System.out.println("ScriptReader: ignoring "+line);
        continue;
      }
      if (line.startsWith(EXPECT_BUILD)) {
    	  // Handle expectBuild first, since it's out of order in the script
    	  executeLine(line);
    	  continue;    	  
      } 
      if (lastLine != null) {
    	  executeLine(lastLine);
      }      
      lastLine = line;
    }
    if (lastLine != null) {
  	  executeLine(lastLine);
    }
  }

  private void executeLine(String line) throws Exception {
	  System.out.println("ScriptReader: "+line);
	  final String[] tokens = Util.collectTokens(line, " ,\n");
	  if (tokens.length == 0) {
		  return;
	  }	  
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
		System.out.println("build FTA");
		new FirstTimeAnalysis(project).run(null);
	} else {
		System.out.println("build workspace");
		ResourcesPlugin.getWorkspace().build(kind, null);
	}
  }
  
  class ExpectBuild extends AbstractCommand {
	public boolean execute(ICommandContext context, String... contents)
			throws Exception {
		final File expected = resolveFile(contents[1]);
		if(expected != null && expected.exists()) {
			JavacDriver.getInstance().setArg(ScriptCommands.EXPECT_BUILD, expected);
		} else {
			throw new FileNotFoundException(contents[1] + " does not exist"); 
		}
		return false;
	}
  }
}
