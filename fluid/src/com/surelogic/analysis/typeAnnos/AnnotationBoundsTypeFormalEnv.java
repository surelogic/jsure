package com.surelogic.analysis.typeAnnos;

import java.util.EnumSet;
import java.util.Set;

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
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode abNode) {
        return abNode.getContainable();
      }
      
      @Override
      public boolean testBounds(
          final AnnotationBoundsNode abNode, final String formalName) {
        return testFormalAgainstNamedTypes(
            formalName, abNode.getContainable());
      }
    },
    
    IMMUTABLE {
      @Override
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode abNode) {
        return abNode.getImmutable();
      }
      
      @Override
      public boolean testBounds(
          final AnnotationBoundsNode abNode, final String formalName) {
        return testFormalAgainstNamedTypes(
            formalName, abNode.getImmutable());
      }
    },
    
    REFERENCE {
      @Override
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode abNode) {
        return abNode.getReference();
      }
      
      @Override
      public boolean testBounds(
          final AnnotationBoundsNode abNode, final String formalName) {
        return testFormalAgainstNamedTypes(
            formalName, abNode.getReference());
      }
    },
    
    THREADSAFE {
      @Override
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode abNode) {
        return abNode.getThreadSafe();
      }
      
      @Override
      public boolean testBounds(
          final AnnotationBoundsNode abNode, final String formalName) {
        return
            testFormalAgainstNamedTypes(
                formalName, abNode.getImmutable()) ||
            testFormalAgainstNamedTypes(
                formalName, abNode.getThreadSafe());
      }
    },
      
    VALUE {
      @Override
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode abNode) {
        return abNode.getValue();
      }
      
      @Override
      public boolean testBounds(
          final AnnotationBoundsNode abNode, final String formalName) {
        return testFormalAgainstNamedTypes(
            formalName, abNode.getValue());
      }
    };
    
    public abstract NamedTypeNode[] getNamedTypes(AnnotationBoundsNode abNode);
    
    public abstract boolean testBounds(AnnotationBoundsNode abNode, String formalName);
  }

  
  

  
  
  private static final EnumSet<Bounds> emptySet = EnumSet.noneOf(Bounds.class);
  private static final EnumSet<Bounds> containableSet = EnumSet.of(Bounds.CONTAINABLE);
  private static final EnumSet<Bounds> immutableSet = EnumSet.of(Bounds.IMMUTABLE);
  private static final EnumSet<Bounds> referenceSet = EnumSet.of(Bounds.REFERENCE);
  private static final EnumSet<Bounds> threadSafeSet = EnumSet.of(Bounds.IMMUTABLE, Bounds.THREADSAFE);
  private static final EnumSet<Bounds> valueSet = EnumSet.of(Bounds.VALUE);
  
  private static final EnumSet<Bounds> notContainableSet = EnumSet.complementOf(containableSet);
  private static final EnumSet<Bounds> notImmutableSet = EnumSet.complementOf(immutableSet);
  private static final EnumSet<Bounds> notReferenceSet = EnumSet.complementOf(referenceSet);
  private static final EnumSet<Bounds> notThreadSafeSet = EnumSet.complementOf(threadSafeSet);
  private static final EnumSet<Bounds> notValueSet = EnumSet.complementOf(valueSet);
  
  

  private static boolean testFormalAgainstNamedTypes(
      final String formalName, final NamedTypeNode[] annotationBounds) {
    for (final NamedTypeNode namedType : annotationBounds) {
      if (namedType.getType().equals(formalName)) {
        return true;
      }
    }
    return false;
  }

  
  /**
   * Formal must be annotated by one of the bounds in oneOf, and must not 
   * be annotated by any of the bounds in noneOf.  So if oneOf = { X, Y }
   * and noneOf = { W, Z }, we want ((X or Y) and not (W or Z)).
   */
  private static boolean testFormalAgainstAnnotationBounds(
      final AnnotationBoundsNode abNode, final String formalName,
      final Set<Bounds> oneOf, final Set<Bounds> noneOf) {
    boolean oneOfFlag = oneOf.isEmpty();  // trivially satisfied if there are no positive requirements
    boolean noneOfFlag = false;
    for (final Bounds b : Bounds.values()) {
      if (oneOf.contains(b)) {
        oneOfFlag |= testFormalAgainstNamedTypes(formalName, b.getNamedTypes(abNode));
      }
      if (noneOf.contains(b)) {
        noneOfFlag |= testFormalAgainstNamedTypes(formalName, b.getNamedTypes(abNode));
      }
    }
    return oneOfFlag && !noneOfFlag;
  }

  private PromiseDrop<? extends IAASTRootNode> isX(
      final IJavaTypeFormal formal, final boolean exclusive, Set<Bounds> oneOf, Set<Bounds> noneOf) {
    final IRNode decl = formal.getDeclaration();
    final String name = TypeFormal.getId(decl);
    final IRNode typeDecl = JJNode.tree.getParent(JJNode.tree.getParent(decl));
    final AnnotationBoundsPromiseDrop abDrop = LockRules.getAnnotationBounds(typeDecl);
    if (abDrop == null) {
      return null;
    } else {
      return testFormalAgainstAnnotationBounds(abDrop.getAAST(), name, oneOf, exclusive ? noneOf : emptySet) ? abDrop : null;
    }
  }

  
  
  
  public PromiseDrop<? extends IAASTRootNode> isContainable(
      final IJavaTypeFormal formal, final boolean exclusive) {
    return isX(formal, exclusive, containableSet, notContainableSet);
  }

  public PromiseDrop<? extends IAASTRootNode> isImmutable(
      final IJavaTypeFormal formal, final boolean exclusive) {
    return isX(formal, exclusive, immutableSet, notImmutableSet);
  }

  public PromiseDrop<? extends IAASTRootNode> isReferenceObject(
      final IJavaTypeFormal formal, final boolean exclusive) {
    return isX(formal, exclusive, referenceSet, notReferenceSet);
  }

  public PromiseDrop<? extends IAASTRootNode> isThreadSafe(
      final IJavaTypeFormal formal, final boolean exclusive) {
    return isX(formal, exclusive, threadSafeSet, notThreadSafeSet);
  }

  public PromiseDrop<? extends IAASTRootNode> isValueObject(
      final IJavaTypeFormal formal, final boolean exclusive) {
    return isX(formal, exclusive, valueSet, notValueSet);
  }
}
