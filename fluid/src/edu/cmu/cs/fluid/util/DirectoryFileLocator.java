/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/DirectoryFileLocator.java,v 1.3 2007/07/20 21:03:12 swhitman Exp $
 */
package edu.cmu.cs.fluid.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;


/**
 * A file locator that looks for files in the directory.
 * @author boyland
 */
public class DirectoryFileLocator extends AbstractFileLocator {

  private File directory;
  
  /**
   * Create a directory locator that uses the current directory
   */
  public DirectoryFileLocator() {
    this(System.getProperty("user.dir"));
  }

  /**
   * Create a directory file locator using the given name
   * for the directory.
   */
  public DirectoryFileLocator(String dirname) {
    this(new File(dirname));
  }
  
  /**
   * Create a directory file locator that will find files in the given directory
   * @param dir directory to look in
   */
  public DirectoryFileLocator(File dir) {
    directory = dir;
  }

  /**
   * Build up the directory structure if it does not already exist; set this 
   * as the new directory file locator 
   * @param name
   * @param mustExist
   * @return
   */
  public void setAndCreateDirPath(String dirname) {
	  File newPath = new File(dirname); 
	  
	  if(!newPath.isDirectory()) {
		  newPath.mkdirs();
	  }
	  
	  directory = newPath;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.util.FileLocator#locateFile(java.lang.String, boolean)
   */
  @Override
  public File locateFile(String name, boolean mustExist) {
    File test = new File(directory, name);
    if (mustExist && canRead(test, false))
      return test;
    else if (!mustExist && canCreateOrWrite(test, false)) return test;
    return null;
  }

  public static boolean canRead(File f, boolean dir) {
    return f.exists() && (dir == f.isDirectory());
  }

  public static boolean canCreateOrWrite(File f, boolean dir) {
    if (f.exists())
      return (dir == f.isDirectory()) && f.canWrite();
    String p = new File(f.getAbsolutePath()).getParent();
    if (p == null)
      return false;
    // System.out.println("Looking at " + p);
    File pdir = new File(p);
    boolean possible = canCreateOrWrite(pdir, true);
    if (possible) {
      if (!pdir.exists())
        pdir.mkdir();
    }
    return possible;
  }
  
  public List<File> listFiles() {
	  return Arrays.asList(directory.listFiles());
  }
}
