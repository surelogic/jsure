package com.surelogic.test.scripting;

import java.util.*;

/**
 * A container for the available commands
 * 
 * @author Edwin
 */
public final class Commands {
  private final Map<String, ICommand> commands = new HashMap<String,ICommand>();
  
  public Commands() {
    commands.put("touchFile", new TouchFile());
    commands.put("saveFile", new SaveFile());
    commands.put("patchFile", new PatchFile());
    commands.put("saveAllFiles", new SaveAllFiles());
    commands.put("createProject", new CreateProject());
    commands.put("openProject", new OpenProject());
    commands.put("addNature", new AddNature());
    commands.put("removeNature", new RemoveNature());
    commands.put("cleanProject", new CleanProject());
    commands.put("closeProject", new CloseProject());
    commands.put("patchFile", new PatchFile());
    commands.put("exportResults", new ExportResults());
    commands.put("compareResults", new CompareResults());
    commands.put("deleteFile", new DeleteFile());
    commands.put("import", new Import());
  }
  
  public ICommand get(String name) {
    ICommand c = commands.get(name);
    if (c == null) {
      return NullCommand.prototype;
    }
    return c;
  }
  
  public ICommand put(String name, ICommand c) {
    ICommand old = commands.put(name, c);
    return old;
  }
}
