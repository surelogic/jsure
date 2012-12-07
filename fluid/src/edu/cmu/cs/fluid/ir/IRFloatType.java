package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.util.Comparator;
import com.surelogic.ThreadSafe;

/** Floats are storable.
 */
@ThreadSafe
public final class IRFloatType implements IRType<Float> , Comparator<Float> {
  private IRFloatType() {
	  // Made private
  }
  public static final IRFloatType prototype = new IRFloatType();
  static {
    IRPersistent.registerIRType(prototype,'F');
  }
  
  public boolean isValid(Object x) {
    return x instanceof Float;
  }

  public int compare( final Float f1, final Float f2 )
  {
    return f1.compareTo( f2 );
  }

  public Comparator<Float> getComparator() 
  {
    return this;
  }

  public void writeValue(Float v, IROutput out) 
     throws IOException
  {
    out.writeFloat(v.floatValue());
  }
  public Float readValue(IRInput in)
     throws IOException
  {
    return Float.valueOf(in.readFloat());
  }
  public void writeType(IROutput out) throws IOException
  {
    out.writeByte('F');
  }
  public IRType<Float> readType(IRInput in) { return this; }

  public Float fromString(String s) {
    return Float.valueOf(s);
  }

  public String toString(final Float f) {
    return f.toString();
  }
}
