package com.surelogic.analysis.alias;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaNullType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil.UpperBoundGetter;

/**
 * May alias analysis that uses type assignment compatibility to determine if
 * two expressions may be aliased.
 */
public final class TypeBasedMayAlias implements IMayAlias {
  private final IBinder binder;
  private final ITypeEnvironment typeEnv;
  private final UpperBoundGetter upperBoundGetter;
  
  
  
  public TypeBasedMayAlias(final IBinder b) {
    binder = b;
    typeEnv = b.getTypeEnvironment();
    upperBoundGetter = new UpperBoundGetter(typeEnv);
  }

  
  
  private boolean testAssignmentCompatibility(final IJavaType type1,
      final IJavaType type2) {
    final boolean assign1to2 = typeEnv.isAssignmentCompatible(type2, type1, null);
    final boolean assign2to1 = typeEnv.isAssignmentCompatible(type1, type2, null);
    return assign1to2 || assign2to1;
  }
  
  private boolean arrayAndArrayOrDeclared(
      final IJavaType type1, final IJavaType type2) {
    return (type1 instanceof IJavaArrayType) && 
      ((type2 instanceof IJavaDeclaredType) ||
          (type2 instanceof IJavaArrayType));
  }
  
  @Override
  public boolean mayAlias(final IRNode expr1, final IRNode expr2) {
    final IJavaType upper1 =
        upperBoundGetter.getUpperBound(binder.getJavaType(expr1));
    final IJavaType upper2 =
        upperBoundGetter.getUpperBound(binder.getJavaType(expr2));
    
    // Null type has only one value, null, that DOES NOT refer to any object, so no aliases
    if (upper1 instanceof IJavaNullType || upper2 instanceof IJavaNullType) {
      return false;
    }
    
    // Primitive values don't have aliases
    if (upper1 instanceof IJavaPrimitiveType || upper2 instanceof IJavaPrimitiveType) {
      return false;
    }
    
    /* Array types can be directly compared against other array types or
     * declared types.  We don't have to worry about general interfaces because
     * all array types implement Clonable and Serializable only.  It is not 
     * possible to extend an array type and add a new interface to it, as is 
     * possible with regular class types. 
     */
    if (arrayAndArrayOrDeclared(upper1, upper2) || 
        arrayAndArrayOrDeclared(upper2, upper1)) {
      return testAssignmentCompatibility(upper1, upper2);
    }
    
    // Two declared types that aren't interfaces may be aliased is they are assignable to each other
    if (upper1 instanceof IJavaDeclaredType && upper2 instanceof IJavaDeclaredType) {
      final IRNode t1 = ((IJavaDeclaredType) upper1).getDeclaration();
      final IRNode t2 = ((IJavaDeclaredType) upper2).getDeclaration();
      if (!TypeUtil.isInterface(t1) && !TypeUtil.isInterface(t2)) {
        return testAssignmentCompatibility(upper1, upper2);
      }
    } 
    
    // Otherwise, may be aliases
    return true;
  }
}
