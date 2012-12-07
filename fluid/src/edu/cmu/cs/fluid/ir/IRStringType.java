/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRStringType.java,v 1.12 2007/05/30 20:35:17 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.util.Comparator;
import com.surelogic.ThreadSafe;

/** The type of slots storing strings.
 */

@ThreadSafe
public class IRStringType implements IRType<String>, Comparator<String> {
  private IRStringType() {
  }
  public static final IRStringType prototype = new IRStringType();
  static {
    IRPersistent.registerIRType(prototype, 'S');
  }
  public boolean isValid(Object x) {
    return x instanceof String;
  }

  public int compare(String o1, String o2) {
    return o1.compareTo(o2);
  }

  public Comparator<String> getComparator() {
    return this;
  }

  public void writeValue(String v, IROutput out) throws IOException {
    out.writeUTF(v);
  }
  public String readValue(IRInput in) throws IOException {
    return in.readUTF();
  }
  public void writeType(IROutput out) throws IOException {
    out.writeByte('S');
  }
  public IRType<String> readType(IRInput in) {
    return this;
  }

  public String fromString(String s) {
    return s;
  }

  public String toString(String o) {
    return o;
  }
}