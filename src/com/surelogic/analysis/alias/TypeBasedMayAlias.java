package com.surelogic.analysis.alias;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaCaptureType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
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
    if (type instanceof IJavaTypeFormal) {
      return getUpperBound(((IJavaTypeFormal) type).getSuperclass(typeEnv));
    } else if (type instanceof IJavaWildcardType) {
      /* I think this case is mute because what we are actually going to see is
       * capture types.
       */
      final IJavaType upperBound = ((IJavaWildcardType) type).getUpperBound();
      return (upperBound == null) ? javaLangObject : getUpperBound(upperBound);
    } else if (type instanceof IJavaCaptureType) {
      final IJavaType upperBound = ((IJavaCaptureType) type).getUpperBound();
      return (upperBound == null) ? javaLangObject : getUpperBound(upperBound);
    } else {
      /* IJavaPrimitiveType, IJavaVoidType, IJavaArrayType, IJavaCaptureType,
       * IJavaIntersectionType, IJavaNullType, IJavaDeclaredType.
       */
      return type;
    }
  }
  
  public boolean mayAlias(final IRNode expr1, final IRNode expr2) {
    final IJavaType upper1 = getUpperBound(binder.getJavaType(expr1));
    final IJavaType upper2 = getUpperBound(binder.getJavaType(expr2));
    final boolean assign1to2 = typeEnv.isAssignmentCompatible(upper2, upper1, null);
    final boolean assign2to1 = typeEnv.isAssignmentCompatible(upper1, upper2, null);
    return assign1to2 || assign2to1;
  }
}
