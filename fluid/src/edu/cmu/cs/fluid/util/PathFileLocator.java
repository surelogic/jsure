/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/PathFileLocator.java,v 1.19 2008/06/24 19:13:12 thallora Exp $ */
package edu.cmu.cs.fluid.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

/** A locator that has a list of directories to try first.
 * If none fit, the current directory is used.
 */
public class PathFileLocator extends AbstractFileLocator {
  /**
	 * Logger for this class
	 */
  private static final Logger LOG = SLLogger.getLogger("fluid");

  private final FileLocator[] flocs;

  public PathFileLocator(FileLocator[] fs) {
    flocs = fs.clone();
  }
  
  public PathFileLocator(File[] ds) {
    flocs = new FileLocator[ds.length];
    for (int i=0; i < ds.length; ++i) {
      flocs[i] = createFileLocator(ds[i]);
    }
  }

  public PathFileLocator(String path) {
    String sep = System.getProperty("path.separator");
    Vector<FileLocator> fs = new Vector<FileLocator>();
    StringTokenizer toks = new StringTokenizer(path, sep);
    while (toks.hasMoreTokens()) {
      String dirString = toks.nextToken();
      fs.addElement(createFileLocator(new File(dirString)));
    }
    flocs = new FileLocator[fs.size()];
    fs.copyInto(flocs);
  }

  private PathFileLocator(FileLocator[] fs, FileLocator floc) {
    flocs = new FileLocator[fs.length + 1];
    System.arraycopy(fs, 0, flocs, 0, fs.length);
    flocs[fs.length] = floc;
  }
  
  private PathFileLocator(FileLocator floc, FileLocator[] fs) {
    flocs = new FileLocator[fs.length + 1];
    System.arraycopy(fs, 0, flocs, 1, fs.length);
    flocs[0] = floc;
  }
  
  /**
   * @param file
   * @return
   */
  private FileLocator createFileLocator(File file) {
    //System.out.println("Creating floc for " + file);
    if (file.isDirectory()) {
      return new DirectoryFileLocator(file);
    } else {
      try {
        //LOG.info("Creating zip file locator for " + file);
        return new ZipFileLocator(file,ZipFileLocator.READ);
      } catch (IOException e) {
        LOG.info("Could not open " + file + ", ignoring it.");
        return NullFileLocator.prototype;
      }
    }
  }


  public File locateFile(String name, boolean mustExist) {
    for (int i=0; i < flocs.length; ++i) {
      File f = flocs[i].locateFile(name,mustExist);
      if (f != null) return f;
    }
    return null;
  }


  public static void main(String args[]) {
    String s = System.getProperty(args[0], ".");
    System.out.println("Property of " + args[0] + " is " + s);
    FileLocator floc = new PathFileLocator(s);
    for (int i = 1; i < args.length; ++i) {
      System.out.println(floc.locateFile(args[i], true).toString());
    }
  }
  
  @Override
  public OutputStream openFileWriteOrNull(String name) {
    for (int i=0; i < flocs.length; ++i) {
      OutputStream os = flocs[i].openFileWriteOrNull(name);
      if (os != null) return os;
    }
    return null;
  }

  @Override
  public InputStream openFileReadOrNull(String name){
    for (int i=0; i < flocs.length; ++i) {
      InputStream is = flocs[i].openFileReadOrNull(name);
      if (is != null) return is;
    }
    return null;
  }

  //TODO
  public Vector<InputStream> openFiles() {
	  Vector<InputStream> ins = new Vector<InputStream>();
	  
	  
	  return ins;
  }
  
  @Override
  public void commit() throws IOException {
    for (int i=0; i < flocs.length; ++i) {
      flocs[i].commit();
    }
  }


  public FileLocator appendToPath(FileLocator floc) {
    if (floc == null) {
      return this;
    }
    return new PathFileLocator(flocs, floc);
  }
  
  public FileLocator prependToPath(FileLocator floc) {
    if (floc == null) {
      return this;
    }
    return new PathFileLocator(floc, flocs);
  }
}