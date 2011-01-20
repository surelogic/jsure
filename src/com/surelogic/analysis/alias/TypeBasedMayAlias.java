package com.surelogic.analysis.alias;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;

/**
 * May alias analysis that uses type assignment compatibility to determine if
 * two expressions may be aliased.
 */
public final class TypeBasedMayAlias implements IMayAlias {
  private final IBinder binder;
  private final ITypeEnvironment typeEnv;
  
  
  
  public TypeBasedMayAlias(final IBinder b) {
    binder = b;
    typeEnv = b.getTypeEnvironment();
  }

  
  
  public boolean mayAlias(final IRNode expr1, final IRNode expr2) {
    /* Two variables are aliased if one can be assigned to the other
     */
    final IJavaType type1 = binder.getJavaType(expr1);
    final IJavaType type2 = binder.getJavaType(expr2);
    final boolean assign1to2 = typeEnv.isAssignmentCompatible(type2, type1, null);
    final boolean assign2to1 = typeEnv.isAssignmentCompatible(type1, type2, null);
    return assign1to2 || assign2to1;
  }
}
