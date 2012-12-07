/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRLocationType.java,v 1.14 2007/05/30 20:35:17 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.util.Comparator;
import com.surelogic.ThreadSafe;

@ThreadSafe
public class IRLocationType implements IRType<IRLocation> {
  public IRLocationType() {
  }
  public static final IRLocationType prototype = new IRLocationType();
  static {
    IRPersistent.registerIRType(prototype, 'L');
  }
  /** Return whether the argument is a value that can be stored
   * as an IRLocation.
   * @return true if an IRLocation or null.
   */
  public boolean isValid(Object x) {
    return x == null || x instanceof IRLocation;
  }

  public Comparator<IRLocation> getComparator() {
    return null;
  }

  /** Write a valid location to an output stream.
   * @param v a valid location
   * @param out the output stream
   * @see #isValid
   * @throws IOException if error happens while writing
   */
  public void writeValue(IRLocation v, IROutput out) throws IOException {
    if (v == null) {
      out.writeInt(-1);
    } else {
      out.writeInt(v.getID());
    }
  }
  /** Read a location from the stream.
   * @return a valid location
   * @param in the input stream.
   * @see #isValid
   * @throws IOException if error happens while reading
   */
  public IRLocation readValue(IRInput in) throws IOException {
    int id = in.readInt();
    if (id == -1) {
      return null;
    } else {
      return IRLocation.get(id);
    }
  }
  public void writeType(IROutput out) throws IOException {
    out.writeByte('L');
  }
  public IRType<IRLocation> readType(IRInput in) {
    return this;
  }

  public IRLocation fromString(String s) {
    return IRLocation.valueOf(s);
  }

  public String toString(IRLocation o) {
    if (o == null)
      return "";

    return o.toString();
  }
}