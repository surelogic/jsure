package com.surelogic.analysis.type.checker;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.java.operator.ReferenceType;
import edu.cmu.cs.fluid.java.operator.ReturnType;
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
  
  /**
   * Get the reference type based on a type expression.
   * 
   * @param referenceTypeExpr
   *          A node whose Operator type is a subtype of {@link ReferenceType}.
   * @return The {@link IType} object described by the type expression.
   * @throws IllegalArgumentException Thrown if the expression does not
   * describe a legal type.
   */
  public IType getReferenceTypeFromExpression(IRNode referenceTypeExpr);

  /**
   * Get the reference type defined by the given type declaration.
   *  
   * @param typeDecl A ClassDeclaration or InterfaceDeclaration node.
   * @return The type corresponding to the declaration.
   */
  public IType getReferenceTypeFromDeclaration(IRNode typeDecl);
  
  /**
   * Get the array type from the given base type and number of dimensions.
   * 
   * @param baseType
   *          The element type of the array.
   * @param dims
   *          The number of dimensions in the array, must be greater than 0.
   * @return The type of the specified array.
   */
  public IType getArrayType(IType baseType, int dims);
  
  
  
  // ======================================================================
  // == Any Type
  // ======================================================================

  /**
   * Get the type from a type expression.
   * 
   * @param A
   *          node whose operator type is a subtype of {@link ReturnType}.
   * @return The type object described by the expression.
   */
  public IType getTypeFromExpression(IRNode typeExpr);
  
  
  
  // ======================================================================
  // == Boxing Conversion
  // ======================================================================
  
  /* XXX: This should be moved somewhere else.  That is, conversions should
   * not be in the type factory, but in some kind of conversion engine.
   */ 
  
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
