package com.surelogic.test.scripting;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

public class ScriptReader implements ICommandContext {
  private Commands commands = new Commands();
  /**
   * Build after every command
   */
  boolean autoBuild = true;
  boolean changed   = false;
  boolean buildNow  = false;
  
  public ScriptReader() {
    commands.put("set", new ICommand() {
      public boolean execute(ICommandContext context, String[] contents) throws Exception {
        if ("autobuild".equals(contents[1])) {
          autoBuild = true;
        }
        return false;
      }  
    });
    commands.put("unset", new ICommand() {
      public boolean execute(ICommandContext context, String[] contents) throws Exception {
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
    final BufferedReader br = new BufferedReader(r);  
    String line;
    while ((line = br.readLine()) != null) {
      String[] tokens = Util.collectTokens(line, " \n");
      if (tokens == null || tokens.length == 0) {
        continue;
      }
      if (tokens[0].startsWith("#")) {
        continue;
      }
      changed = changed || commands.get(tokens[0]).execute(this, tokens);
      if (buildNow || changed && autoBuild) {
        changed  = false;
        buildNow = false;
        build();
      }
    }
  }

  private void build() throws CoreException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    for(IProject p : root.getProjects()) {
      // check if files changed    
      p.build(IncrementalProjectBuilder.AUTO_BUILD, null);
    }
  }
}
