/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/control/PrimitiveExceptionLabel.java,v 1.5 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.java.control;


public class PrimitiveExceptionLabel extends ExceptionLabel {
  public final String name;
  public PrimitiveExceptionLabel(String n) {
    name = n;
  }
  @Override
  public String toString() {
    return name;
  }
  public static final PrimitiveExceptionLabel primitiveArithmeticException =
    new PrimitiveExceptionLabel("java.lang.ArithmeticException");
  public static final PrimitiveExceptionLabel primitiveArrayStoreException =
    new PrimitiveExceptionLabel("java.lang.ArrayStoreException");
  public static final PrimitiveExceptionLabel primitiveClassCastException =
    new PrimitiveExceptionLabel("java.lang.ClassCastException");
  public static final PrimitiveExceptionLabel
    primitiveIndexOutOfBoundsException =
    new PrimitiveExceptionLabel("java.lang.IndexOutOfBoundsException");
  public static final PrimitiveExceptionLabel
    primitiveNegativeArraySizeException =
    new PrimitiveExceptionLabel("java.lang.NegativeArraySizeException");
  public static final PrimitiveExceptionLabel primitiveNullPointerException =
    new PrimitiveExceptionLabel("java.lang.NullPointerException");
  public static final PrimitiveExceptionLabel assertionError =
    new PrimitiveExceptionLabel("java.lang.AssertionError");
}
