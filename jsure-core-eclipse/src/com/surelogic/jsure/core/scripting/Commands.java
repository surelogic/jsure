package com.surelogic.jsure.core.scripting;

import java.util.*;

/**
 * A container for the available commands
 * 
 * @author Edwin
 */
public final class Commands implements ScriptCommands {
  private final Map<String, ICommand> commands = new HashMap<String,ICommand>();
  
  public Commands() {
    commands.put(TOUCH_FILE, new TouchFile());
    commands.put("saveFile", new SaveFile());
    commands.put(PATCH_FILE, new PatchFile());
    commands.put("saveAllFiles", new SaveAllFiles());
    commands.put(CREATE_PROJECT, new CreateProject());
    commands.put(OPEN_PROJECT, new OpenProject());
    commands.put(DELETE_PROJECT, new DeleteProject());
    commands.put(CLEANUP_DROPS, new CleanupDrops());
    commands.put("cleanProject", new CleanProject());
    commands.put(CLOSE_PROJECT, new CloseProject());
    commands.put(PATCH_FILE, new PatchFile());
    commands.put(EXPORT_RESULTS, new ExportResults());
    commands.put(COMPARE_RESULTS, new CompareResults());
    commands.put(DELETE_FILE, new DeleteFile());
    commands.put(IMPORT, new Import());
  }
  
  public ICommand get(String name) {
    ICommand c = commands.get(name);
    if (c == null) {
      throw new UnsupportedOperationException("No command matching "+name);
      //return NullCommand.prototype;
    }
    return c;
  }
  
  public ICommand put(String name, ICommand c) {
    ICommand old = commands.put(name, c);
    return old;
  }
}
