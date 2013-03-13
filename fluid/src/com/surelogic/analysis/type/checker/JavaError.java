package com.surelogic.analysis.type.checker;

/**
 * Enumerations of errors from plain vanilla Java type checking as defined in 
 * the JLS.
 */
public enum JavaError implements ITypeError {
  NAME_NOT_RESOLVABLE("Could not resolve name"),
  
  NOT_CONVERTIBLE_TO_NUMERIC_TYPE("Type is not convertible to a numeric type"),
  NOT_CONVERTIBLE_TO_INTEGRAL_TYPE("Type is not convertible to an integral type"),
  
  NOT_INT("Type is not int"),
  NOT_REFERENCE_TYPE("Type is not a reference type"),
  NOT_ARRAY_TYPE("Type is not an array"),
  NOT_BOOLEAN_TYPE("Type is not boolean or java.lang.Boolean"),
  NOT_INTEGRAL_TYPE("Type is not a primitive integral type"),
  
  NOT_APPLICABLE("Operator is not applicable to the given types"),
  
  VOID_NOT_ALLOWED("Expression may not be void"),
  
  A("");
  
  private JavaError(final String msg) {
    message = msg;
  }
  
  private final String message;
  
  @Override
  public final String getMessage() {
    return message;
  }
}
