package com.surelogic.analysis.type.checker;

public interface IConversionEngine {
  // ======================================================================
  // == ¤5.1.7 Boxing Conversion
  // ======================================================================
  
//  /**
//   * Perform a boxing conversion on the given primitive type.
//   * 
//   * @param primitiveType
//   *          The type to box convert.
//   * @return The boxed type.
//   * @throws IllegalArgumentException
//   *           Thrown if the given type represents the <code>void</code> type.
//   */
//  public IType box(IPrimitiveType primitiveType);



  // ======================================================================
  // == ¤5.1.10 Capture Conversion
  // ======================================================================
  
  /**
   * Perform capture conversion on the given type.
   * 
   * @param type
   *          The type to convert.
   * @return The type after capture conversion.
   */
  public IType capture(IType type);



//  // ======================================================================
//  // == ¤5.6.1 Unary Numeric Promotion
//  // ======================================================================
//  
//  /**
//   * Perform unary numeric promotion on the given type.
//   * 
//   * @param type
//   *          the type to promote.
//   * @return The promoted type.
//   * @exception IllegalArgumentException
//   *              Thrown if unary numeric promotion is not applicable to the
//   *              given type.
//   */
//  public IType unaryNumericPromotion(IType type);
}
