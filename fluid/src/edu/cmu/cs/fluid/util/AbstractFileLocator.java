/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/AbstractFileLocator.java,v 1.1 2004/06/25 15:20:06 boyland Exp $
 */
package edu.cmu.cs.fluid.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * And abstract file locator that implements the open methods by
 * calling the open...OrNull methods and implements the latter methods by calling
 * locateFile.  It also assumes that commits require no change.
 * @author boyland
 */
public abstract class AbstractFileLocator implements FileLocator {

  public AbstractFileLocator() {
    super();
  }

  @Override
  public OutputStream openFileWriteOrNull(String name) {
    File file = this.locateFile(name, false);
    if (file == null) return null;
    try {
      return new BufferedOutputStream(new FileOutputStream(file));
    } catch (FileNotFoundException e) {
      return null;
    }     
  }

  @Override
  public OutputStream openFileWrite(String name) throws IOException {
    OutputStream os = openFileWriteOrNull(name);
    if (os == null) {
      throw new FileNotFoundException("Could not open " + name + " for writing");
    }
    return os;
  }

  @Override
  public InputStream openFileReadOrNull(String name) {
    File file = this.locateFile(name, true);
    if (file == null) return null;
    try {
      return new BufferedInputStream(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  @Override
  public InputStream openFileRead(String name) throws IOException {
    InputStream is = openFileReadOrNull(name);
    if (is == null) {
      throw new FileNotFoundException("Could not open " + name + " for reading");
    }
    return is;
  }

  @Override
  public void commit() throws IOException {
    // do nothing
  }

}
