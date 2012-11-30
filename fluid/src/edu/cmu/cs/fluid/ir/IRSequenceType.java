/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRSequenceType.java,v 1.19 2007/05/25 02:12:41 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.util.Comparator;
import java.util.logging.Logger;

import com.surelogic.ThreadSafe;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.NotImplemented;

/** The type of storable homogenous sequences.
 * Each element is typed too.
 */
@ThreadSafe
public class IRSequenceType<T> implements IRCompoundType<IRSequence<T>> {
  private static final Logger LOG = Logger.getLogger("FLUID.ir.type");
  
  private final IRType<T> elementType;
  public IRSequenceType(IRType<T> elemType) {
    elementType = elemType;
  }
  
  public static final IRType<IRSequence<Object>> objectSequenceType = 
    new IRSequenceType<Object>(IRObjectType.prototype);
  
  public static final IRType<IRSequence<IRNode>> nodeSequenceType = 
    new IRSequenceType<IRNode>(IRNodeType.prototype);
  
  static {
    IRPersistent.registerIRType(new IRSequenceType<Void>(IRUnitType.prototype), '[');
  }

  public IRType<T> getElementType() {
    return elementType;
  }
  public IRType<T> getType(int ignored) {
    return elementType;
  }

  @SuppressWarnings("unchecked")
  public boolean isValid(Object value) {
    if (value == null)
      return true;
    if (!(value instanceof IRSequence))
      return false;
    IRSequence<T> seq = (IRSequence<T>) value;
    for (IRLocation loc = seq.firstLocation();
      loc != null;
      loc = seq.nextLocation(loc)) {
      if (seq.validAt(loc))
        if (!elementType.isValid(seq.elementAt(loc)))
          return false;
    }
    return true;
  }

  public Comparator<IRSequence<T>> getComparator() {
    return null;
  }

  public void writeValue(IRSequence<T> seq, IROutput out) throws IOException {
    // sequences have identity:
    if (out.writeCachedObject(seq))
      return;
    //! Currently we only support arrays and lists
    //! (and empty sequences)
    if (seq.isVariable()) {
      out.writeByte('L'); // assume a list
    } else if (seq.size() == 0) {
      out.writeByte('E');
    } else {
      out.writeByte('A'); // assume an array
    }
    seq.writeValue(out);
    seq.writeContents(this, out);
  }

  public IRSequence<T> readValue(IRInput in) throws IOException {
    return readValueHelper(in, null);
  }

  public IRSequence<T> readValue(IRInput in, IRSequence<T> currentValue) throws IOException {
    if (currentValue == null)
      throw new NullPointerException("null is not a legal compound value");
    return readValueHelper(in, currentValue);
  }

  @SuppressWarnings("unchecked")
  private IRSequence<T> readValueHelper(IRInput in, IRSequence<T> current)
    throws IOException {
    IRSequence<T> obj = (IRSequence<T>) in.readCachedObject();
    if (obj != null) {
      if (current != obj && current != null) {
        LOG.warning("");
      }
      return obj;
    }
    byte b = in.readByte();
    IRSequence<T> seq;
    if (b == 'A') {
      seq = IRArray.readValue(in, current);
    } else if (b == 'L') {
      seq = IRList.readValue(in, current);
    } else if (b == 'E') {
      seq = EmptyIRSequence.prototype.readValue(in, current);
    } else {
      throw new FluidError("Unknown sequence class " + b);
    }
    in.cacheReadObject(seq);
    seq.readContents(this, in);
    return seq;
  }

  public void writeType(IROutput out) throws IOException {
    out.writeByte('[');
    out.writeIRType(elementType);
  }
  @SuppressWarnings("unchecked")
  public IRType<IRSequence<T>> readType(IRInput in) throws IOException {
    return new IRSequenceType<T>(in.readIRType());
  }

  /** @exception fluid.NotImplemented */
  public IRSequence<T> fromString(String s) {
    throw new NotImplemented("fluid.ir.IRSequenceType.fromString()");
  }

  /** @exception fluid.NotImplemented */
  public String toString(IRSequence<T> o) {
    throw new NotImplemented("fluid.ir.IRSequenceType.toString()");
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof IRSequenceType) {
      IRSequenceType t = (IRSequenceType) o;
      return this.getElementType().equals(t.getElementType());
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    return getElementType().hashCode();
  }
}