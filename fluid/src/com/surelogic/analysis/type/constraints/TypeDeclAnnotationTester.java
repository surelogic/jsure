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

public abstract class TypeDeclAnnotationTester {
  private final Map<IJavaType, ResultFolderDrop> annoBoundsFolders;
  protected final ITypeFormalEnv formalEnv;
  private final ITypeEnvironment typeEnv;
  protected final boolean exclusive;
  
  private final IJavaDeclaredType javaLangObject;
  private final Set<IRNode> tested = new HashSet<IRNode>();
  private final Set<ProofDrop> trusts = new HashSet<ProofDrop>();
  private final Set<IRNode> failed = new HashSet<IRNode>();
  
  
  
  protected TypeDeclAnnotationTester(
      final IBinder binder, final ITypeFormalEnv fe, 
      final Map<IJavaType, ResultFolderDrop> folders, final boolean ex) {
    formalEnv = fe;
    final ITypeEnvironment te = binder.getTypeEnvironment();
    typeEnv = te;
    annoBoundsFolders = folders;
    javaLangObject = te.getObjectType();
    exclusive = ex;
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
  
  
  
  protected final boolean testDeclaredType(final IJavaDeclaredType type) {
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
  
  
 
  protected abstract boolean testArrayType(IJavaArrayType type);
  
  protected abstract ProofDrop testTypeDeclaration(
      IRNode type);
  
  protected abstract PromiseDrop<?> testFormalAgainstAnnotationBounds(
      IJavaTypeFormal formal);
}
