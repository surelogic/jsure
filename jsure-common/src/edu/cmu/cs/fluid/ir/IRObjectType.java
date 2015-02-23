/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRObjectType.java,v 1.6 2007/05/30 20:35:17 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.util.Comparator;

import com.surelogic.Immutable;

import edu.cmu.cs.fluid.NotImplemented;

/** The type of slots storing Java objects.
 */
@Immutable
public class IRObjectType<T> implements IRType<T> {
  public IRObjectType() {
  }
  public static final IRObjectType<Object> prototype = new IRObjectType<Object>();
  static {
    IRPersistent.registerIRType(prototype, ' ');
  }
  public boolean isValid(Object x) {
    return true;
  }

  public Comparator<T> getComparator() {
    throw new NotImplemented("");
  }

  public void writeValue(T v, IROutput out) throws IOException {
    throw new NotImplemented("");
  }
  public T readValue(IRInput in) throws IOException {
    throw new NotImplemented("");
  }
  public void writeType(IROutput out) throws IOException {
    out.writeByte(' ');
  }
  public IRType<T> readType(IRInput in) {
    return this;
  }

  public T fromString(String s) {
    throw new NotImplemented("");
  }

  public String toString(T o) {
    return o.toString();
  }
}