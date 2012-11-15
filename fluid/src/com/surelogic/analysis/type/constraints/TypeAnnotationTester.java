package com.surelogic.analysis.type.constraints;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProofDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;

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

public final class TypeAnnotationTester {
  private final TypeAnnotations typeAnnotation;
  private final Map<IJavaType, ResultFolderDrop> annoBoundsFolders;
  private final ITypeEnvironment typeEnv;
  
  private final IJavaDeclaredType javaLangObject;
  private final Set<IRNode> tested = new HashSet<IRNode>();
  private final Set<ProofDrop> trusts = new HashSet<ProofDrop>();
  private final Set<IRNode> failed = new HashSet<IRNode>();
  
  
  
  public TypeAnnotationTester(final TypeAnnotations ta,
      final IBinder binder, final Map<IJavaType, ResultFolderDrop> folders) {
    typeAnnotation = ta;
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
  
  
  
  public final boolean testFieldDeclarationType(final IJavaType type) {
    return testType(type, typeAnnotation.forFieldDeclaration());
  }
  
  public final boolean testFinalObjectType(final IJavaType type) {
    return testType(type, typeAnnotation.forFinalObject());
  }
  
  public final boolean testParameterizedTypeActual(final IJavaType type) {
    return testType(type, typeAnnotation.forParameterizedTypeActual());
  }

  
  
  private final boolean testType(final IJavaType type, final TypeTester typeTester) {
    if (type instanceof IJavaNullType) {
      return false;
    } else if (type instanceof IJavaPrimitiveType) {
      return false;
    } else if (type instanceof IJavaVoidType) {
      return false;
    } else if (type instanceof IJavaDeclaredType) {
      return testDeclaredType((IJavaDeclaredType) type, typeTester);
    } else if (type instanceof IJavaArrayType) {
      return typeTester.testArrayType((IJavaArrayType) type);
    } else if (type instanceof IJavaCaptureType) {
      final IJavaType upper = ((IJavaCaptureType) type).getUpperBound();
      testType((upper == null) ? javaLangObject : upper, typeTester);
    } else if (type instanceof IJavaIntersectionType) {
      final IJavaIntersectionType intType = (IJavaIntersectionType) type;
      final boolean first = testType(intType.getPrimarySupertype(), typeTester);
      final boolean second = testType(intType.getSecondarySupertype(), typeTester);
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
          typeTester.testFormalAgainstAnnotationBounds((IJavaTypeFormal) type);
      if (bound != null) {
        trusts.add(bound);
        return true;
      } else {
        // Test the upperbound
        final IJavaType upper = ((IJavaTypeFormal) type).getSuperclass(typeEnv);
        return testType((upper == null) ? javaLangObject : upper, typeTester);
      }
    } else if (type instanceof IJavaUnionType) {
      // Can't get the least upper bound, use object instead
      return testType(javaLangObject, typeTester);
    } else if (type instanceof IJavaWildcardType) {
      // dead case?  Turned into Capture types, I think
      final IJavaType upper = ((IJavaWildcardType) type).getUpperBound();
      return testType((upper == null) ? javaLangObject : upper, typeTester);
    } 
    // shouldn't get here?
    return false;
  }
  
  
  
  private boolean testDeclaredType(final IJavaDeclaredType type, final TypeTester typeTester) {
    final IRNode typeDecl = type.getDeclaration();
    tested.add(typeDecl);
    final ProofDrop drop = typeTester.testTypeDeclaration(typeDecl);
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
}
