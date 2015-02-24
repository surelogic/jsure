/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRIntegerType.java,v 1.11 2007/05/30 20:35:17 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.util.Comparator;
import com.surelogic.ThreadSafe;

/** Integers are storable.
 */
@ThreadSafe
public class IRIntegerType implements IRType<Integer> , Comparator<Integer> {
  private IRIntegerType() {}
  public static final IRIntegerType prototype = new IRIntegerType();
  static {
    IRPersistent.registerIRType(prototype,'I');
  }
  
  public boolean isValid(Object x) {
    return x instanceof Integer;
  }

  public int compare( final Integer i1, final Integer i2 )
  {
    return i1.compareTo( i2 );
  }

  public Comparator<Integer> getComparator() 
  {
    return this;
  }

  public void writeValue(Integer v, IROutput out) 
     throws IOException
  {
    out.writeInt((v).intValue());
  }
  public Integer readValue(IRInput in)
     throws IOException
  {
    return new Integer(in.readInt());
  }
  public void writeType(IROutput out) throws IOException
  {
    out.writeByte('I');
  }
  public IRType<Integer> readType(IRInput in) { return this; }

  public Integer fromString(String s) {
    return Integer.valueOf(s);
  }

  public String toString(Integer i) {
    return i.toString();
  }
}
