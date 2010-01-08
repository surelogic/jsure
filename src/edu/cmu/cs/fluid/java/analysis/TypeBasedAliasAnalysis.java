package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;

public final class TypeBasedAliasAnalysis implements IAliasAnalysis {
  /**
   * We don't actually care about the flow unit that contains the
   * <code>before</code> node, so we have a single method factory that we always
   * return.
   */
  private final MethodFactory methodFactory = new MethodFactory() {
    public Method getMustAliasMethod(final IRNode before) {
      return new Method() {
        public boolean aliases(final IRNode e1, final IRNode e2) {
          return mustAlias(e1, e2, before);
        }
      };
    }
    
    public Method getMayAliasMethod(final IRNode before) {
      return new Method() {
        public boolean aliases(final IRNode e1, final IRNode e2) {
          return mayAlias(e1, e2, before);
        }
      };
    }
  };
  
  private final IBinder binder;
  
  
  
  public TypeBasedAliasAnalysis(final IBinder b) {
    binder = b;
  }
  
  public MethodFactory getMethodFactory(final IRNode flowUnit) {
    return methodFactory;
  }

  
  
  public boolean mayAlias(
      IRNode expr1, IRNode expr2, IRNode before, IRNode constructorContext) {
    return mayAlias(expr1, expr2, before);
  }

  /**
   * Always returns <code>false</code>.
   */
  public boolean mustAlias(
      IRNode expr1, IRNode expr2, IRNode before, IRNode constructorContext) {
    return mustAlias(expr1, expr2, before);
  }
  
  
  
  /**
   * Implements a simple may alias check by testing if the types
   * of the two expressions.  They cannot be aliases if neither type
   * is a subtype of the other.  The <code>before</code> parameter is
   * ignored.
   */
  private boolean mayAlias(IRNode expr1, IRNode expr2, IRNode before) {
    final IJavaType type1 = binder.getJavaType(expr1);
    final IJavaType type2 = binder.getJavaType(expr2);
    final ITypeEnvironment typeEnv = binder.getTypeEnvironment();
    if (typeEnv.isSubType(type1, type2)) {
      return true;
    } else {
      return typeEnv.isSubType(type2, type1);
    }
  }
  
  private boolean mustAlias(IRNode expr1, IRNode expr2, IRNode before) {
    return false;
  }}
