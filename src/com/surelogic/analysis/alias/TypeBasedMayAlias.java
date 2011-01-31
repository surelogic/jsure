package com.surelogic.analysis.alias;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaCaptureType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaIntersectionType;
import edu.cmu.cs.fluid.java.bind.IJavaNullType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.bind.IJavaVoidType;
import edu.cmu.cs.fluid.java.bind.IJavaWildcardType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;

/**
 * May alias analysis that uses type assignment compatibility to determine if
 * two expressions may be aliased.
 */
public final class TypeBasedMayAlias implements IMayAlias {
  private static final String JAVA_LANG_OBJECT = "java.lang.Object";
  
  private final IBinder binder;
  private final ITypeEnvironment typeEnv;
  private final IJavaType javaLangObject;
  
  
  
  public TypeBasedMayAlias(final IBinder b) {
    binder = b;
    typeEnv = b.getTypeEnvironment();
    javaLangObject = typeEnv.findJavaTypeByName(JAVA_LANG_OBJECT);
  }

  
  
  private IJavaType getUpperBound(final IJavaType type) {
    /* 2011-01-31: I'm not sure this is correct.  I'm concerned about
     * wildcards, intersection types, and capture types, and their interaction,
     * but I cannot test this right now because I don't think we generate
     * capture types appropriately.  
     */
//    if (type instanceof IJavaPrimitiveType) {
//      return type;
//    } else if (type instanceof IJavaVoidType) {
//      return type;
//    } else if (type instanceof IJavaArrayType) {
//      return type;
//    } else if (type instanceof IJavaCaptureType) {
//      return type;
//    } else if (type instanceof IJavaIntersectionType) {
//      return type;
//    } else if (type instanceof IJavaNullType) {
//      return type;
//    } else if (type instanceof IJavaDeclaredType) {
//      return type;
    if (type instanceof IJavaTypeFormal) {
      return getUpperBound(((IJavaTypeFormal) type).getSuperclass(typeEnv));
    } else if (type instanceof IJavaWildcardType) {
      final IJavaType upperBound = ((IJavaWildcardType) type).getUpperBound();
      return (upperBound == null) ? javaLangObject : getUpperBound(upperBound);
    } else {
      /* IJavaPrimitiveType, IJavaVoidType, IJavaArrayType, IJavaCaptureType,
       * IJavaIntersectionType, IJavaNullType, IJavaDeclaredType.
       */
      return type;
    }
  }
  
  public boolean mayAlias(final IRNode expr1, final IRNode expr2) {
    /* Two variables are aliased if they may refer to objects that can be
     * assigned to each other.  We need to be careful about type parameters
     * because we could have a variant of the following situation:
     * 
     *   public <A, B> void m(A a, B b) { ... }
     *   
     *   public void test() { ...
     *     C c = ...;
     *     this.<C, C>m(c, c);
     *   }
     *   
     * That is, the types A and B are not assignable to each other, but they
     * may refer to types that are due to the formal parameters.  In this case, 
     * we have A and B both bound to the type C, so that in reality we have 
     * C is assignable to C.  So we have to take into account the upper bound 
     * on the type that may be assigned to the type variables.
     */
    final IJavaType type1 = binder.getJavaType(expr1);
    final IJavaType type2 = binder.getJavaType(expr2);
    
    final IJavaType upper1 = getUpperBound(type1);
    final IJavaType upper2 = getUpperBound(type2);
    
    final boolean assign1to2 = typeEnv.isAssignmentCompatible(upper2, upper1, null);
    final boolean assign2to1 = typeEnv.isAssignmentCompatible(upper1, upper2, null);
    return assign1to2 || assign2to1;
  }
}
