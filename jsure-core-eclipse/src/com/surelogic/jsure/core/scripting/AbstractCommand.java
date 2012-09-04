package com.surelogic.jsure.core.scripting;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * Contains various utility methods to find projects/files in the workspace
 * 
 * @author Edwin
 */
public abstract class AbstractCommand implements ICommand {
  protected static final byte[] noBytes = new byte[0];
  
  @Override
  public boolean succeeded() {
	  return true;
  }
  
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
  
  protected IFile resolveIFile(String name) {
    return resolveIFile(name, false);
  }
  
  protected IFile resolveIFile(String name, boolean create) {
    final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IPath path          = Path.fromOSString(name);
    IFile file          = root.getFile(path);
    try {
    	try {
    		if (checkIfExists(file, create)) {
    			return file;
    		}
    	} catch(CoreException e) {
    		// ignore for now
    	}
		final String[] tokens = Util.collectTokens(name, "/");
    	try {
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
    			if (checkIfExists(file, create)) {
    				return file;
    			}
    		}
    	} catch (IllegalArgumentException e) {
    		System.out.println("Bad project: "+tokens[0]+" -- "+name);
    		throw e;
    	}
    } catch (CoreException e) {
		return null;
	}
    return null;
  }
  
  private boolean checkIfExists(IFile file, boolean create) throws CoreException {
	  if (file.exists()) {
		  return true;
	  } else if (create) {
		  /*
		  if (file.getName().contains(".")) {
			  // Only if it's supposed to be a file			  
			  // TODO file.touch(null);
		  } else {
			  // TODO how to create a directory?
			  // p.getFolder()
		  }
		  */
		  return true;
	  }
	  return false;
  }
  
  protected File resolveFile(ICommandContext context, String name) {
	  return resolveFile(context, name, false);
  }
  
  protected File resolveFile(ICommandContext context, String name, boolean create) {
	  File f = new File(name);
	  if (create) {
		  try {
			  f.createNewFile();
		  } catch(IOException e) {
			  // Ignore for now
			  //e.printStackTrace();
		  }
	  }
	  if (!f.exists()) {
		  // Check for a relative path (to the script)
		  /*
		  final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		  final File workspace = new File(root.getLocation().toOSString());
		  f = new File(workspace, name);
		  */
		  Object dir = context.getArgument(ScriptReader.SCRIPT_DIR);
		  if (dir != null) {
			  File root = (File) dir;
			  if (name.startsWith("/"+root.getName())) {
				  root = root.getParentFile();
			  }
			  f = new File(root, name);
			  if (f.exists()) {
				  return f;
			  }			  
		  }
		  final IFile oracleFile = resolveIFile(name, create);
		  if (oracleFile == null) {
			  System.out.println("Couldn't find file: "+name);
			  return null;
		  }
		  final URI pathURI = oracleFile.getLocationURI();
		  if (pathURI == null) {
			  System.out.println("Couldn't get URI: "+name);
			  return null;
		  }
		  final String path  = pathURI.getPath();
		  f = new File(path);
	  }
	  return f;
  }
}
