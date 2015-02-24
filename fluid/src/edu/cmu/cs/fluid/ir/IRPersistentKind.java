/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRPersistentKind.java,v 1.5 2003/07/02 20:19:15 thallora Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.surelogic.ThreadSafe;

/* A class for reading and writing persistent identifiers
 * in other persistent files.  The identifiers are used for
 * imports and exports.
 * @see IRPersistent
 */
@ThreadSafe
public interface IRPersistentKind {
  /** Write a reference to a persistent object.
   * @param p the persistent object to refer to
   * @param out the output stream onto which to write the reference
   * @throws IOException if error writing to stream
   */
  public void writePersistentReference(IRPersistent p, DataOutput out)
    throws IOException;
  /** Read a reference to a persistent object and return it.
   * This object must be defined.
   * @param in input stream containing reference
   * @throws IOException if error reading stream or loading object.
   */
  public IRPersistent readPersistentReference(DataInput in) throws IOException;
}