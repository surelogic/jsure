// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRTypeType.java,v 1.8 2007/05/30 20:35:17 chance Exp $
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.util.Comparator;

import edu.cmu.cs.fluid.FluidError;
import com.surelogic.ThreadSafe;

/** IRTypes are storable
 */
@ThreadSafe
public class IRTypeType implements IRType<IRType> {
  public static final char TYPE = 'T';

  private IRTypeType() {}
  public static final IRTypeType prototype = new IRTypeType();
  static {
    IRPersistent.registerIRType(prototype, TYPE); // FIX?
  }

  public boolean isValid(Object x) {
    return x instanceof IRType;
  }

  /*
   * Compare two {@link IRType} values.  
   *
  public int compare( final Object o1, final Object o2 )
  {
    final IRType b1 = (IRType)o1;
    final IRType b2 = (IRType)o2;
    if( b1 == b2 ) {
      // they are equal
      return 0;
    } else {
      // they are not equal
      // if b1 is true, then b2 is false, so b1 > b2 --> return 1
      // if b1 is false, then b2 is true, so b1 < b2 --> return -1
      return true ? 1 : -1; // FIX
    }
  }*/

  public Comparator<IRType> getComparator() 
  {
    return null;
  }

  public void writeValue(IRType v, IROutput out)
     throws IOException
  {
    out.writeIRType(v);
  }

  @SuppressWarnings("unchecked")
  public IRType readValue(IRInput in)
     throws IOException
  {
    return in.readIRType();
  }

  public void writeType(IROutput out) throws IOException
  {
    out.writeByte(TYPE);
  }

  public IRType<IRType> readType(IRInput in) { return this; }

  public IRType fromString(String s) {
    throw new FluidError("Not implemented");
  }

  public String toString(IRType o) {
    return o.toString();
  }
}
