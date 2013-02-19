package com.surelogic.analysis.type.checker;

import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Factory interface for generating {@link IType types} for the type checker.
 */
public interface ITypeFactory {
  // ======================================================================
  // == Primitive types
  // ======================================================================
  
  /** Get the null type */
  public IPrimitiveType getNullType();
  
  /** Get the type for <code>void</code>. */
  public IPrimitiveType getVoidType();
  
  /** Get the type for <code>boolean</code>. */
  public IPrimitiveType getBooleanType();
  
  /** Get the type for <code>byte</code>. */
  public IPrimitiveType getByteType();
  /** Get the type for <code>char</code>. */
  public IPrimitiveType getCharType();
  /** Get the type for <code>short</code>. */
  public IPrimitiveType getShort();
  /** Get the type for <code>int</code>. */
  public IPrimitiveType getIntType();
  /** Get the type for <code>long</code>. */
  public IPrimitiveType getLongType();
  
  /** Get the type for <code>float</code>. */
  public IPrimitiveType getFloatType();
  /** Get the type for <code>double</code>. */
  public IPrimitiveType getDoubleType();

  /**
   * Generate the primitive type based on the parse operator.
   * 
   * @param primitiveTypeOp
   *          An operator that is a leaf subtype of {@link PrimitiveType}.
   * 
   * @return The {@link IPrimitiveType} equivalent of the given operator.
   * @throws IllegalArgumentException
   *           Thrown if <code>primitiveTypeOp</code> is not a leaf subtype of
   *           {@link PrimitiveType}.
   */
  public IPrimitiveType getPrimitiveType(Operator primitiveTypeOp);

  

  // ======================================================================
  // == Reference types
  // ======================================================================
  
  /** Get the type for <code>java.lang.Object</code>. */
  public IType getObjectType();
  /** Get the type for <code>java.lang.String</code>. */
  public IType getStringType();

  /**
   * Get the type for <code>Class&lt;T&gt;</code>.
   * 
   * @param type The type <code>T</code> that the class object represents.
   * @return The type object for <code>Class&lt;T&gt;</code>
   */
  public IType getClassType(IType type);
  
//  public IType getNamedType(String name);

  
  
  // ======================================================================
  // == Boxing Conversion
  // ======================================================================
  
  /**
   * Perform a boxing conversion on the given primitive type.
   * 
   * @param primitiveType
   *          The type to box convert.
   * @return The boxed type.
   * @throws IllegalArgumentException
   *           Thrown if the given type represents the <code>void</code> type.
   */
  public IType box(IPrimitiveType primitiveType);
}
