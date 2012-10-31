package com.surelogic.analysis.type.constraints;

import java.util.EnumSet;
import java.util.Set;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.promise.AnnotationBoundsNode;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.AnnotationBoundsPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ContainablePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.parse.JJNode;

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
      public boolean testContainable(final ContainablePromiseDrop cDrop) {
        return false;
      }
    },
    
    IMMUTABLE {
      @Override
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode abNode) {
        return abNode.getImmutable();
      }
      
      @Override
      public boolean testContainable(final ContainablePromiseDrop cDrop) {
        return false;
      }
    },
    
    REFERENCE {
      @Override
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode abNode) {
        return abNode.getReference();
      }
      
      @Override
      public boolean testContainable(final ContainablePromiseDrop cDrop) {
        return cDrop != null && cDrop.allowReferenceObject();
      }
    },
    
    THREADSAFE {
      @Override
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode abNode) {
        return abNode.getThreadSafe();
      }
      
      @Override
      public boolean testContainable(final ContainablePromiseDrop cDrop) {
        return cDrop != null;
      }
    },
      
    VALUE {
      @Override
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode abNode) {
        return abNode.getValue();
      }
      
      @Override
      public boolean testContainable(final ContainablePromiseDrop cDrop) {
        return false;
      }
    };
    
    public abstract NamedTypeNode[] getNamedTypes(AnnotationBoundsNode abNode);
    
    public abstract boolean testContainable(ContainablePromiseDrop cDrop);
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

  private static boolean testFormalAgainstContainable(
      final ContainablePromiseDrop cDrop, 
      final Set<Bounds> oneOf, final Set<Bounds> noneOf) {
    boolean oneOfFlag = oneOf.isEmpty();  // trivially satisfied if there are no positive requirements
    boolean noneOfFlag = false;
    for (final Bounds b : Bounds.values()) {
      if (oneOf.contains(b)) {
        oneOfFlag |= b.testContainable(cDrop);
      }
      if (noneOf.contains(b)) {
        noneOfFlag |= b.testContainable(cDrop);
      }
    }
    return oneOfFlag && !noneOfFlag;
  }

  private PromiseDrop<?> isX(
      final IJavaTypeFormal formal, final boolean exclusively, Set<Bounds> oneOf, Set<Bounds> noneOf) {
    final IRNode decl = formal.getDeclaration();
    final String name = TypeFormal.getId(decl);
    final IRNode typeDecl = JJNode.tree.getParent(JJNode.tree.getParent(decl));
    
    /* Favor explicit annotation bounds over those implied by 
     * @Containable
     */
    PromiseDrop<?> result = null;
    final AnnotationBoundsPromiseDrop abDrop = LockRules.getAnnotationBounds(typeDecl);
    if (abDrop != null) {
      result = testFormalAgainstAnnotationBounds(abDrop.getAAST(), name, oneOf, exclusively ? noneOf : emptySet) ? abDrop : null;
    }
    
    if (result == null) {
      final ContainablePromiseDrop cDrop = LockRules.getContainableImplementation(typeDecl);
      if (cDrop != null) {
        result = testFormalAgainstContainable(cDrop, oneOf, exclusively ? noneOf : emptySet) ? cDrop : null;
      }
    }
    
    return result;
  }
  
  public PromiseDrop<?> isContainable(
      final IJavaTypeFormal formal, final boolean exclusively) {
    return isX(formal, exclusively, containableSet, notContainableSet);
  }

  public PromiseDrop<?> isImmutable(
      final IJavaTypeFormal formal, final boolean exclusively) {
    return isX(formal, exclusively, immutableSet, notImmutableSet);
  }

  public PromiseDrop<?> isReferenceObject(
      final IJavaTypeFormal formal, final boolean exclusively) {
    return isX(formal, exclusively, referenceSet, notReferenceSet);
  }

  public PromiseDrop<?> isThreadSafe(
      final IJavaTypeFormal formal, final boolean exclusively) {
    return isX(formal, exclusively, threadSafeSet, notThreadSafeSet);
  }

  public PromiseDrop<?> isValueObject(
      final IJavaTypeFormal formal, final boolean exclusively) {
    return isX(formal, exclusively, valueSet, notValueSet);
  }
}
