/*
 * IRLongType.java
 *
 * Created on August 16, 2000, 2:52 PM
 */

package edu.cmu.cs.fluid.ir;

/**
 *
 * @author  smzia
 * @version 
 */
import java.io.IOException;
import java.util.Comparator;
import com.surelogic.ThreadSafe;

/** Longs are storable.
 */
@ThreadSafe
public class IRLongType implements IRType<Long> , Comparator<Long> {
  private IRLongType() {}
  public static final IRLongType prototype = new IRLongType();
  static {
    IRPersistent.registerIRType(prototype,'o');
  }
  
  public boolean isValid(Object x) {
    return x instanceof Long;
  }

  public int compare( final Long i1, final Long i2 )
  {
    return i1.compareTo( i2 );
  }

  public Comparator<Long> getComparator() 
  {
    return this;
  }

  public void writeValue(Long v, IROutput out) 
     throws IOException
  {
    out.writeLong(v.longValue());
  }
  public Long readValue(IRInput in)
     throws IOException
  {
    return new Long(in.readLong());
  }
  public void writeType(IROutput out) throws IOException
  {
    out.writeByte('o');
  }
  public IRType<Long> readType(IRInput in) { return this; }

  public Long fromString(String s) {
    return Long.valueOf(s);
  }

  public String toString(Long i) {
    return i.toString();
  }
}