package com.surelogic.analysis.type.constraints;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.promise.AnnotationBoundsNode;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProofDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.AnnotationBoundsPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ContainablePromiseDrop;

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
import edu.cmu.cs.fluid.java.bind.IJavaUnionType;
import edu.cmu.cs.fluid.java.bind.IJavaVoidType;
import edu.cmu.cs.fluid.java.bind.IJavaWildcardType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.parse.JJNode;

public abstract class TypeDeclAnnotationTester {
  private final Map<IJavaType, ResultFolderDrop> annoBoundsFolders;
  private final ITypeEnvironment typeEnv;
  
  private final IJavaDeclaredType javaLangObject;
  private final Set<IRNode> tested = new HashSet<IRNode>();
  private final Set<ProofDrop> trusts = new HashSet<ProofDrop>();
  private final Set<IRNode> failed = new HashSet<IRNode>();
  
  
  
  protected TypeDeclAnnotationTester(
      final IBinder binder, final Map<IJavaType, ResultFolderDrop> folders) {
    final ITypeEnvironment te = binder.getTypeEnvironment();
    typeEnv = te;
    annoBoundsFolders = folders;
    javaLangObject = te.getObjectType();
  }
  
  
  
  public final Iterable<IRNode> getTested() {
    return tested;
  }
  
  public final Set<ProofDrop> getTrusts() {
    return trusts;
  }
  
  public final Iterable<IRNode> getFailed() {
    return failed;
  }

  
  
  public final boolean testType(final IJavaType type) {
    if (type instanceof IJavaNullType) {
      return false;
    } else if (type instanceof IJavaPrimitiveType) {
      return false;
    } else if (type instanceof IJavaVoidType) {
      return false;
    } else if (type instanceof IJavaDeclaredType) {
      return testDeclaredType((IJavaDeclaredType) type);
    } else if (type instanceof IJavaArrayType) {
      return testArrayType((IJavaArrayType) type);
    } else if (type instanceof IJavaCaptureType) {
      final IJavaType lower = ((IJavaCaptureType) type).getLowerBound();
      testType((lower == null) ? javaLangObject : lower);
    } else if (type instanceof IJavaIntersectionType) {
      final IJavaIntersectionType intType = (IJavaIntersectionType) type;
      final boolean first = testType(intType.getPrimarySupertype());
      final boolean second = testType(intType.getSecondarySupertype());
      /*
       * Intersection implies AND, so you would think that we should conjoin
       * the results below. But an interface that is not annotated with X may
       * have X-annotated implementations. So mixing an unannotated interface
       * into the intersection doesn't kill the possibility of X-ness. If the
       * class portion of the intersection is not-X, then really the whole
       * intersection should be false, but it's not possible to have a
       * implementation that is X where the class is not-X and an interface is
       * X (the sanity checking fails in this case), so it doesn't matter if we
       * let this case through here.
       */
      return first || second;
    } else if (type instanceof IJavaTypeFormal) {
      // First check the formal against annotation bounds
      final PromiseDrop<?> bound = 
          testFormalAgainstAnnotationBounds((IJavaTypeFormal) type);
      if (bound != null) {
        trusts.add(bound);
        return true;
      } else {
        // Test the upperbound
        final IJavaType upper = ((IJavaTypeFormal) type).getSuperclass(typeEnv);
        return testType((upper == null) ? javaLangObject : upper);
      }
    } else if (type instanceof IJavaUnionType) {
      // Can't get the least upper bound, use object instead
      return testType(javaLangObject);
    } else if (type instanceof IJavaWildcardType) {
      // dead case?  Turned into Capture types, I think
      final IJavaType lower = ((IJavaWildcardType) type).getLowerBound();
      return testType((lower == null) ? javaLangObject : lower);
    } 
    // shouldn't get here?
    return false;
  }
  
  
  
  final boolean testDeclaredType(final IJavaDeclaredType type) {
    final IRNode typeDecl = type.getDeclaration();
    tested.add(typeDecl);
    final ProofDrop drop = testTypeDeclaration(typeDecl);
    if (drop != null) {
      trusts.add(drop);
      final ResultFolderDrop annoBounds = annoBoundsFolders.get(type);
      if (annoBounds != null) {
        trusts.add(annoBounds);
      }
      return true;
    } else {
      failed.add(typeDecl);
      return false;
    }
  }
  
  
 
  abstract boolean testArrayType(IJavaArrayType type);
  
  abstract ProofDrop testTypeDeclaration(
      IRNode type);
  
  abstract PromiseDrop<?> testFormalAgainstAnnotationBounds(
      IJavaTypeFormal formal);
  
  
  // ======================================================================
  
  
  
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
  
  /**
   * Test if the given type formal is annotated to be containable.
   * 
   * @param formal
   *          The type formal to test
   * @param exclusively
   *          <code>true</code> if the type formal must not be allowed to be
   *          annotated with anything else.
   * 
   * @return The promise drop of the annotation that allows us to conclude that
   *         this type formal is containable or <code>null</code> if the type
   *         formal is not containable.
   */
  public PromiseDrop<?> isContainable(
      final IJavaTypeFormal formal, final boolean exclusively) {
    return isX(formal, exclusively, containableSet, notContainableSet);
  }

  /**
   * Test if the given type formal is annotated to be immutable.
   * 
   * @param formal
   *          The type formal to test
   * @param exclusively
   *          <code>true</code> if the type formal must not be allowed to be
   *          annotated with anything else.
   * 
   * @return The promise drop of the annotation that allows us to conclude that
   *         this type formal is immutable or <code>null</code> if the type
   *         formal is not immutable.
   */
  public PromiseDrop<?> isImmutable(
      final IJavaTypeFormal formal, final boolean exclusively) {
    return isX(formal, exclusively, immutableSet, notImmutableSet);
  }

  /**
   * Test if the given type formal is annotated to be reference object.
   * 
   * @param formal
   *          The type formal to test
   * @param exclusively
   *          <code>true</code> if the type formal must not be allowed to be
   *          annotated with anything else.
   * 
   * @return The promise drop of the annotation that allows us to conclude that
   *         this type formal is reference object or <code>null</code> if the type
   *         formal is not reference object.
   */
  public PromiseDrop<?> isReferenceObject(
      final IJavaTypeFormal formal, final boolean exclusively) {
    return isX(formal, exclusively, referenceSet, notReferenceSet);
  }

  /**
   * Test if the given type formal is annotated to be thread safe.  If the 
   * type formal is annotated immutable, then it is also considered to be
   * thread safe.
   * 
   * @param formal
   *          The type formal to test
   * @param exclusively
   *          <code>true</code> if the type formal must not be allowed to be
   *          annotated with anything else.
   * 
   * @return The promise drop of the annotation that allows us to conclude that
   *         this type formal is thread safe or <code>null</code> if the type
   *         formal is not thread safe.
   */
  public PromiseDrop<?> isThreadSafe(
      final IJavaTypeFormal formal, final boolean exclusively) {
    return isX(formal, exclusively, threadSafeSet, notThreadSafeSet);
  }

  /**
   * Test if the given type formal is annotated to be value object.
   * 
   * @param formal
   *          The type formal to test
   * @param exclusively
   *          <code>true</code> if the type formal must not be allowed to be
   *          annotated with anything else.
   * 
   * @return The promise drop of the annotation that allows us to conclude that
   *         this type formal is value object or <code>null</code> if the type
   *         formal is not value object.
   */
  public PromiseDrop<?> isValueObject(
      final IJavaTypeFormal formal, final boolean exclusively) {
    return isX(formal, exclusively, valueSet, notValueSet);
  }
}
