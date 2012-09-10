package com.surelogic.analysis.concurrency.util;

import java.util.HashSet;
import java.util.Set;

import com.surelogic.aast.IAASTRootNode;

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
import edu.cmu.cs.fluid.sea.PromiseDrop;

public abstract class TypeDeclAnnotationTester {
  protected final ITypeFormalEnv formalEnv;
  private final ITypeEnvironment typeEnv;
  protected final boolean exclusive;
  
  private final IJavaDeclaredType javaLangObject;
  private final Set<IRNode> tested = new HashSet<IRNode>();
  private final Set<PromiseDrop<? extends IAASTRootNode>> promises = 
      new HashSet<PromiseDrop<? extends IAASTRootNode>>();
  private final Set<IRNode> failed = new HashSet<IRNode>();
  
  
  
  protected TypeDeclAnnotationTester(
      final IBinder binder, final ITypeFormalEnv fe, final boolean ex) {
    formalEnv = fe;
    final ITypeEnvironment te = binder.getTypeEnvironment();
    typeEnv = te;
    javaLangObject = te.getObjectType();
    exclusive = ex;
  }
  
  
  
  public final Iterable<IRNode> getTested() {
    return tested;
  }
  
  public final Set<PromiseDrop<? extends IAASTRootNode>> getPromises() {
    return promises;
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
      return testDeclaredType(((IJavaDeclaredType) type).getDeclaration());
    } else if (type instanceof IJavaArrayType) {
      return testArrayType((IJavaArrayType) type);
    } else if (type instanceof IJavaCaptureType) {
      final IJavaType upper = ((IJavaCaptureType) type).getUpperBound();
      testType((upper == null) ? javaLangObject : upper);
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
      final PromiseDrop<? extends IAASTRootNode> bound =
          testFormalAgainstAnnotationBounds((IJavaTypeFormal) type);
      if (bound != null) {
        promises.add(bound);
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
      final IJavaType upper = ((IJavaWildcardType) type).getUpperBound();
      testType((upper == null) ? javaLangObject : upper);
    } 
    // shouldn't get here?
    return false;
  }
  
  
  
  protected final boolean testDeclaredType(final IRNode type) {
    tested.add(type);
    final PromiseDrop<? extends IAASTRootNode> drop = testTypeDeclaration(type);
    if (drop != null) {
      promises.add(drop);
      return true;
    } else {
      failed.add(type);
      return false;
    }
  }
  
  
 
  protected abstract boolean testArrayType(IJavaArrayType type);
  
  protected abstract PromiseDrop<? extends IAASTRootNode> testTypeDeclaration(
      IRNode type);
  
  protected abstract PromiseDrop<? extends IAASTRootNode> testFormalAgainstAnnotationBounds(
      IJavaTypeFormal formal);
}
