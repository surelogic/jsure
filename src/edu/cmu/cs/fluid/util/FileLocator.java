/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/FileLocator.java,v 1.5 2004/06/25 15:20:06 boyland Exp $ */
package edu.cmu.cs.fluid.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface FileLocator {
  /** Return a File for the following name.
   * @param mustExist if true then the file must exist and be
   * a normal file, otherwise it must be writeable.
   * @return null if cannot be found.
   */
  public File locateFile(String name, boolean mustExist);

  /** Return a OutputStream for the following name.
   * @throws IOException if it is not createable or not writeable.
   */
  public OutputStream openFileWrite(String name)
       throws IOException;

  /** Return a InputStream for the following name.
   * @throws IOException if it is not readable
   */
  public InputStream openFileRead(String name)
       throws IOException;

  /** Return a OutputStream for the following name.
   * @return null if cannot be done.
   */
  public OutputStream openFileWriteOrNull(String name);

  /** Return a InputStream for the following name.
   * @return null if cannot be done
   */
  public InputStream openFileReadOrNull(String name);

  /** All pending writes are complete.  The file system should
   * be made ready to receive reads through this file locator.
   * @throws IOException
   */
  public void commit() throws IOException;
}
