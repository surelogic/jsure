package edu.cmu.cs.fluid.java.bind;

import com.surelogic.ThreadSafe;
import com.surelogic.analysis.IIRProject;
import com.surelogic.common.Pair;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.*;

@ThreadSafe
public interface ITypeEnvironment {
	/** The Tree used to build all the ASTs used here */
  SyntaxTreeInterface parsetree = JJNode.tree;

  /**
   * Return what version of Java we can handle (5,6,7,8...)
   * @return Java version we can handle.
   */
  int getMajorJavaVersion();
  
  public abstract IJavaClassTable getClassTable();

	/** @return A possibly incomplete collection of the fully qualified type names
	 *  known to exist
	 * @deprecated unclear what this set is supposed to mean
	 */
	//Set getTypes();
	
	/** @return A possibly incomplete collection of ASTs known to exist 
	 * @deprecated do not use: forces eager loading of ASTs
	 **/	 
	//Collection getTypeASTs();

	/** @return The IBinder that corresponds to this ITypeEnvironment */
  IBinder getBinder();
  
  /**
   * @return The IBindHelper that corresponds to this ITypeEnvironment, if any
   */
  IBindHelper getBindHelper();

	/** 
	 * Find an outer-level type given its complete name.
	 * @param qname A qualified name separated by '.'
	 * @return The corresponding AST */
	IRNode findNamedType(String qname);
	
	IRNode findNamedType(String qname, IRNode context);

	/**
	 * Find the direct subclasses.  
	 * Use recursively to find more distant subtypes
	 */
	public Iterable<IRNode> getRawSubclasses(IRNode type);
	
	/*
	 * @param baseType The base type for the array (e.g., Object for Object[][])
	 * @param dims The number of dimensions (e.g., 2 for Object[][])
	 * @return The AST representing the array type (cached)
	 * @deprecated use JavaTypeFactory.getArrayType(IJavaType,int)
	 */
	//IRNode findArrayType(IRNode baseType, int dims);
	
  /**
   * Return the IJavaType corresponding to a type name
   */
  public IJavaType findJavaTypeByName(final String type);
  
  public IJavaType findJavaTypeByName(final String type, IRNode useSite);
  
  /**
   * Return the root of the type hierarchy: Object.
   * Convenience method (that probably should be cached).
   * @return declared type for java.lang.Object.
   */
  public IJavaDeclaredType getObjectType() ;

  /**
   * Return the artificial class that declares arrays.
   * Convenience method (assuming name of special class is known)
   * @return IRNode for special class declaration for arrays.
   */
  public IRNode getArrayClassDeclaration();

  /**
   * Return the type of string literals.
   * Convenience method (that should probably be cached).
   * @return declared type for java.lang.String.
   */
  public IJavaDeclaredType getStringType();

	/**
	 * Get the immediate supertypes of the given type.
	 * If it has no super types (for example, the type
	 * is primitive, or a null type), then an empty iterator is returned.
	 * This method never returns null.
	 * @param t type to get super types for
	 * @return iterator of all immediate supertypes.
	 */
	public Iteratable<IJavaType> getSuperTypes(IJavaType t);
	
	public IJavaDeclaredType getSuperclass(IJavaDeclaredType t);
	
	/** 
	 * @param s The potential subtype
	 * @param t The type being checked
	 * @return true if s is a subtype of t, false otherwise
	 * @deprecated use isSubType(IJavaType,IJavaType)
	 */
	//boolean isSubType(IRNode s, IRNode t);
	
	/** 
	 * @param s The potential subtype
	 * @param t The type being checked
	 * @return true if s is a subtype of t, false otherwise
	 */
	boolean isSubType(IJavaType s, IJavaType t);
	
	/**
	 * Like isSubType, ignoring generics
	 */
	boolean isRawSubType(IJavaType s, IJavaType t);
	
	/** 
	 * Check if an assignment is compatible.
   * This causes an error if not satisfied:
	 * 1. identity conversion
	 * 2. widening primitive or reference conversion
	 * 3. narrowing primitive conversion if t1 is int, t2 is byte/short/char, n2
	 *    is a constant
	 * 
	 * @param t1 The type of the variable that would be assigned to
	 * @param t2 The type of the expression being assigned
	 * @param n2 The expression being assigned
	 * 
	 * <a href="http://java.sun.com/docs/books/jls/first_edition/html/5.doc.html#170768">
	 * JLS section 5.2 under "Assignment Conversion"
	 * </a>
	 * @deprecated not implemented
	 */
	//void checkIfAssignmentCompatible(IRNode t1, IRNode t2, IRNode n2);
  
  /**
   * @param t1
   * @param t2
   * @param n2
   * @return whether comaptible
   * @deprecated use isAssignmentCompatible(IJavaType,IJavaType,IRNode)
   */
  //boolean isAssignmentCompatible(IRNode t1, IRNode t2, IRNode n2);
	
	/** 
	 * Determine whether an assignment is compatible. <ol>
	 * <li> identity conversion
	 * <li> widening primitive or reference conversion
	 * <li> narrowing primitive conversion if t1 is int, t2 is byte/short/char, n2
	 *    is a constant
	 * </ol>
	 * See <a href="http://java.sun.com/docs/books/jls/first_edition/html/5.doc.html#170768">
	 * JLS section 5.2 under "Assignment Conversion"
	 * </a> or <a href="http://java.sun.com/docs/books/jls/second_edition/html/conversions.doc.html#184206">
	 * JLS (2nd ed.) Section 5.2: Assignment Conversion
	 * </a>
	 * 
	 * @param t1 The type of the variable that would be assigned to
	 * @param t2 The type of the expression being assigned
	 * @param n2 The expression being assigned
	 * @return true if compatible.
	 */
  boolean isAssignmentCompatible(IJavaType t1, IJavaType t2, IRNode n2);
  
  boolean isAssignmentCompatible(IJavaType[] types1, IJavaType[] types2);
  
  /** 
   * Determine whether an assignment is compatible. <ol>
   * <li> identity conversion
   * <li> widening primitive or reference conversion (possibly after boxing/unboxing)
   * </ol>
   * See <a href="http://java.sun.com/docs/books/jls/third_edition/html/conversions.html#5.3">
   * JLS third edition, section 5.3 under "Method Invocation Conversion"
   * </a> 
   * 
   * @param t1 The type of the variable that would be assigned to
   * @param t2 The type of the expression being assigned
   * @return true if compatible.
   */
  boolean isCallCompatible(IJavaType param, IJavaType arg);
  
  boolean isCallCompatible(IJavaType[] types1, IJavaType[] types2);
  
  /// Java 8 methods: (mainly concerning IJavaFunctionType)
  
  IJavaFunctionType computeErasure(IJavaFunctionType ft);

  /**
   * If the interface is a functional interface, return its descriptor.
   * Otherwise return null.  Meets the Java specification.
   * @param idecl node of the interface declaration
   * @return descriptor (if a functional interface) or null (if not)
   */
  public IJavaFunctionType isFunctionalInterface(IRNode idecl);
  
  /**
   * Return a function type if the given reference type is 
   * a "functional type" as described in the Java 8 specification.
   * (Formerly this was called "functional interface type").
   * XXX: The implementation does not exactly match the spec.
   * See comments in {@link AbstractTypeEnvironment#isFunctionalType(IJavaType)}.
   * @param t reference type to consider
   * @return function type if it is indeed a "functional type",
   * or null otherwise
   * 
   * (§9.8)
   */
  IJavaFunctionType isFunctionalType(IJavaType t);
  
  boolean isSameSignature(IJavaFunctionType ft1, IJavaFunctionType ft2);

  boolean isSubsignature(IJavaFunctionType ft1, IJavaFunctionType ft2);
  
  boolean isOverrideEquivalent(IJavaFunctionType ft1, IJavaFunctionType ft2);
  
  boolean isReturnTypeSubstitutable(IJavaFunctionType ft1, IJavaFunctionType ft2);
  
  /**
   * Can a method of type ft1 be used to implement a method of type ft2?
   * Ignoring void, can we implement a method with ft2's signatures
   * if the body is <code> return ft1(args...) </code>.
   * TODO: we probably want to return a map of bindings of type variables.
   * @param ft1 first function type, must not be null
   * @param ft2 second function type, must not be null
   * @param ikind coercions permitted, must not be null
   * @return whether ft1 is call compatoble with ft2
   */
  boolean isCallCompatible(IJavaFunctionType ft1, IJavaFunctionType ft2, InvocationKind ikind);
  
  /**
   * Three kinds of invocation checks:
   * STRICT = no un/boxing or variable arity arguments
   * LOOSE = un/boxing permitted, but not variable arity arguments
   * VARIABLE = un/boxing and varioable arity permitted.
   */
  public static enum InvocationKind { 
	  STRICT, LOOSE, VARIABLE;
	  public boolean canBoxUnbox() {
		  return this != STRICT;
	  }
	  public boolean isVariable() {
		  return this == VARIABLE;
	  }
  };

  
	/**
	 * In fluid/configuration, perhaps it should be the directory component
	 * where all the package's classes live.
	 * @param name
	 * @return The canonical PackageDeclaration (possibly annotated) for the package
	 */
	IRNode findPackage(String name, IRNode context);
	
	Iterable<Pair<String,IRNode>> getPackages();

  /**
   * @deprecated this field will go away.
   */
  /*
  public static final SlotInfo qnameSI = 
    TypeUtil.getSimpleSlotInfo("TypeEnvironment.Qname", IRStringType.prototype, "");
  */
  
  /**
   * @deprecated this field will go away
   */
  /*
  public static final SlotInfo indexSI = 
    TypeUtil.getSimpleSlotInfo("TypeEnvironment.index", IRStringType.prototype, "");
    */
	
	/**
	 * Return the erasure if t is a parameterized type
	 * Otherwise, return t
	 */
	IJavaType computeErasure(IJavaType t);

	IJavaType convertNodeTypeToIJavaType(IRNode nodeType);
	
	/**
	 * Return the "this" type for this declaration, what "this"
	 * means inside this class.  The correct type actuals and
	 * outer type are inferred from the structure.  This method doesn't care
	 * about "static"; it's only interested in proper nesting and polymorphism. 
	 * @param tdecl type declaration node
	 * @return type of "this" within this class/interface.
	 */
	IJavaSourceRefType getMyThisType(IRNode typeDecl);
	
	void clearCaches(boolean clearAll);
	
	IIRProject getProject();

	void addTypesInCU(IRNode root);

}
