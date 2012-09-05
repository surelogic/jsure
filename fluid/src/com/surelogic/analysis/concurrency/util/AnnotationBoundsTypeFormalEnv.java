package com.surelogic.analysis.concurrency.util;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.promise.AnnotationBoundsNode;
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.AnnotationBoundsPromiseDrop;

/**
 * Environment for type formals that used AnnotationBounds annotations to
 * determine how type formals are annotated.
 */
public enum AnnotationBoundsTypeFormalEnv implements ITypeFormalEnv {
  INSTANCE;
  
  
  
  private enum Bounds {
    CONTAINABLE {
      @Override
      public boolean testBounds(
          final AnnotationBoundsNode abNode, final String formalName) {
        return testFormalAgainstAnnotationBounds(
            formalName, abNode.getContainable());
      }
    },
    
    IMMUTABLE {
      @Override
      public boolean testBounds(
          final AnnotationBoundsNode abNode, final String formalName) {
        return testFormalAgainstAnnotationBounds(
            formalName, abNode.getImmutable());
      }
    },
    
    THREADSAFE {
      @Override
      public boolean testBounds(
          final AnnotationBoundsNode abNode, final String formalName) {
        return
            testFormalAgainstAnnotationBounds(
                formalName, abNode.getImmutable()) ||
            testFormalAgainstAnnotationBounds(
                formalName, abNode.getThreadSafe());
      }
    };
    
    private static boolean testFormalAgainstAnnotationBounds(
        final String formalName, final NamedTypeNode[] annotationBounds) {
      for (final NamedTypeNode namedType : annotationBounds) {
        if (namedType.getType().equals(formalName)) {
          return true;
        }
      }
      return false;
    }
    
    public abstract boolean testBounds(AnnotationBoundsNode abNode, String formalName);
  }

  
  
  private PromiseDrop<? extends IAASTRootNode> isX(final Bounds bounds, final IJavaTypeFormal formal) {
    final IRNode decl = formal.getDeclaration();
    final String name = TypeFormal.getId(decl);
    final IRNode typeDecl = JJNode.tree.getParent(JJNode.tree.getParent(decl));
    final AnnotationBoundsPromiseDrop abDrop = LockRules.getAnnotationBounds(typeDecl);
    if (abDrop == null) {
      return null;
    } else {
      return bounds.testBounds(abDrop.getAAST(), name) ? abDrop : null;
    }
  }

  
  
  
  public PromiseDrop<? extends IAASTRootNode> isContainable(final IJavaTypeFormal formal) {
    return isX(Bounds.CONTAINABLE, formal);
  }

  public PromiseDrop<? extends IAASTRootNode> isImmutable(final IJavaTypeFormal formal) {
    return isX(Bounds.IMMUTABLE, formal);
  }

  public PromiseDrop<? extends IAASTRootNode> isThreadSafe(final IJavaTypeFormal formal) {
    return isX(Bounds.THREADSAFE, formal);
  }
}
