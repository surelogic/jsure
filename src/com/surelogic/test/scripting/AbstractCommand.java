package com.surelogic.test.scripting;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * Contains various utility methods to find projects/files in the workspace
 * 
 * @author Edwin
 */
public abstract class AbstractCommand implements ICommand {
  protected static final byte[] noBytes = new byte[0];
  
  protected IProject resolveProject(String name) {
    return resolveProject(name, false);
  }
    
  protected IProject resolveProject(String name, boolean create) {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    for(IProject p : root.getProjects()) {
      if (p.getName().equals(name)) {
        return p;
      }
    }
    if (create) {
      return root.getProject(name);
    }
    return null;
  }
  
  protected IFile resolveFile(String name) {
    return resolveFile(name, false);
  }
  
  protected IFile resolveFile(String name, boolean create) {
    final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IPath path          = Path.fromOSString(name);
    IFile file          = root.getFile(path);
    if (file.exists()) {
      return file;
    }    
    final String[] tokens     = Util.collectTokens(name, "/");
    if (tokens.length == 0) {
      return null;
    }    
    final IProject p = root.getProject(tokens[0]);
    if (p != null) {
      path = p.getProjectRelativePath();
      for(int i=1; i<tokens.length; i++) {
        path = path.append(tokens[i]);
      }
      file = p.getFile(path);
      if (create || file.exists()) {
        return file;
      }
    }
    return null;
  }
}
