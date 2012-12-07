/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRBooleanType.java,v 1.10 2007/05/30 20:35:17 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.util.Comparator;
import com.surelogic.ThreadSafe;

/** Booleans are storable.
 */
@ThreadSafe
public class IRBooleanType implements IRType<Boolean>, Comparator<Boolean> {
  private IRBooleanType() {}
  public static final IRBooleanType prototype = new IRBooleanType();
  static {
    IRPersistent.registerIRType(prototype,'B');
  }

  public boolean isValid(Object x) {
    return x instanceof Boolean;
  }

  /**
   * Compare two {@link java.lang.Boolean} values.  Ordered 
   * with <code>true</code> being greater than <code>false</code>.
   */
  public int compare( final Boolean b1, final Boolean b2 )
  {
    if( b1.booleanValue() == b2.booleanValue() ) {
      // they are equal
      return 0;
    } else {
      // they are not equal
      // if b1 is true, then b2 is false, so b1 > b2 --> return 1
      // if b1 is false, then b2 is true, so b1 < b2 --> return -1
      return b1.booleanValue() ? 1 : -1;
    }
  }

  public Comparator<Boolean> getComparator() 
  {
    return this;
  }

  public void writeValue(Boolean v, IROutput out)
     throws IOException
  {
    out.writeBoolean(v.booleanValue());
  }
  public Boolean readValue(IRInput in)
     throws IOException
  {
    return in.readBoolean() ? Boolean.TRUE : Boolean.FALSE;
  }

  public void writeType(IROutput out) throws IOException
  {
    out.writeByte('B');
  }
  public IRType<Boolean> readType(IRInput in) { return this; }

  public Boolean fromString(String s) {
    //return (s.compareToIgnoreCase("true")==0) ? Boolean.TRUE : Boolean.FALSE;
    return Boolean.valueOf(s);
  }

  public String toString(final Boolean b) {
    return b.toString();
  }
}
