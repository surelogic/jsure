package com.surelogic.analysis.type.constraints;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProofDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ArrayType;
import edu.cmu.cs.fluid.java.operator.MoreBounds;
import edu.cmu.cs.fluid.java.operator.NamedType;
import edu.cmu.cs.fluid.java.operator.ParameterizedType;
import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.java.operator.TypeRef;
import edu.cmu.cs.fluid.java.operator.WildcardExtendsType;
import edu.cmu.cs.fluid.java.operator.WildcardSuperType;
import edu.cmu.cs.fluid.java.operator.WildcardType;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public final class TypeAnnotationTester {
  private final TypeAnnotations typeAnnotation;
  /** 
   * key is an IRNode with operator type ParameterizedType 
   */
  private final Map<IRNode, ResultFolderDrop> annoBoundsFolders;
  private final IBinder binder;
  
  private final IRNode javaLangObject;
  private final Set<IRNode> tested = new HashSet<IRNode>();
  private final Set<ProofDrop> trusts = new HashSet<ProofDrop>();
  private final Set<IRNode> failed = new HashSet<IRNode>();
  
  
  
  public TypeAnnotationTester(final TypeAnnotations ta,
      final IBinder b, final Map<IRNode, ResultFolderDrop> folders) {
    typeAnnotation = ta;
    binder = b;
    annoBoundsFolders = folders;
    javaLangObject = b.getTypeEnvironment().getObjectType().getDeclaration();
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
  
  
  
  public final boolean testFieldDeclarationType(final IRNode typeNode) {
    return testType(typeNode, typeAnnotation.forFieldDeclaration());
  }
  
  public final boolean testFinalObjectType(final IRNode typeNode) {
    return testType(typeNode, typeAnnotation.forFinalObject());
  }
  
  public final boolean testParameterizedTypeActual(final IRNode typeNode) {
    return testType(typeNode, typeAnnotation.forParameterizedTypeActual());
  }

  
  
  private boolean testType(final IRNode typeNode, final TypeTester typeTester) {
    /* Edwin says I should never see a CaptureType or NameType node.
     * 
     * Given that this method is meant to be used on field types, actual types,
     * and the type of a new expression, I should also never see a UnionType 
     * or VarArgsType.
     */
    final Operator typeOp = JJNode.tree.getOperator(typeNode);
    if (PrimitiveType.prototype.includes(typeOp)) {
      return false;
    } else if (NamedType.prototype.includes(typeOp)) {
      return testBoundName(typeTester, binder.getBinding(typeNode));
    } else if (ArrayType.prototype.includes(typeOp)) {
      // TODO: fix this
      return false;
//      return typeTester.testArrayType((IJavaArrayType) type);      
    } else if (TypeRef.prototype.includes(typeOp)) {
      return testBoundName(typeTester, binder.getBinding(typeNode));
    } else if (ParameterizedType.prototype.includes(typeOp)) {
      return testDeclaredType(typeTester,
          binder.getBinding(ParameterizedType.getBase(typeNode)), typeNode);
    } else if (WildcardExtendsType.prototype.includes(typeOp)) {
      return testType(WildcardExtendsType.getUpper(typeNode), typeTester);
    } else if (WildcardSuperType.prototype.includes(typeOp)) {
      // Upper bound is java.lang.Object
      return testDeclaredType(typeTester, javaLangObject, null);
    } else if (WildcardType.prototype.includes(typeOp)) {
      // Must test after extends/super or else we catch them too. 
      // Upper bound is java.lang.Object
      return testDeclaredType(typeTester, javaLangObject, null);
    }
    
    // shouldn't get here
    return false;
  }
  
  private boolean testBoundName(
      final TypeTester typeTester, final IRNode boundName) {
    if (TypeFormal.prototype.includes(boundName)) {
      // First check the formal against annotation bounds
      final PromiseDrop<?> bound = 
          typeTester.testFormalAgainstAnnotationBounds(boundName);
      if (bound != null) {
        trusts.add(bound);
        return true;
      } else { // Test the intersection super type
        /*
         * Intersection implies AND, so you would think that we should conjoin the
         * results below. But an interface that is not annotated with X may have
         * X-annotated implementations. So mixing an unannotated interface into
         * the intersection doesn't kill the possibility of X-ness. If the class
         * portion of the intersection is not-X, then really the whole
         * intersection should be false, but it's not possible to have a
         * implementation that is X where the class is not-X and an interface is X
         * (the sanity checking fails in this case), so it doesn't matter if we
         * let this case through here.
         */
        boolean result = false;
        for (final IRNode conjunctName : MoreBounds.getBoundIterator(TypeFormal.getBounds(boundName))) {
          result |= testType(conjunctName, typeTester);
        }
        return result;
      }
    } else {
      // type declaration
      return testDeclaredType(typeTester, boundName, null);
    }
  }
  
  
//  private final boolean testType(final IJavaType type, final TypeTester typeTester) {
//    if (type instanceof IJavaNullType) {
//      return false;
//    } else if (type instanceof IJavaPrimitiveType) {
//      return false;
//    } else if (type instanceof IJavaVoidType) {
//      return false;
//    } else if (type instanceof IJavaDeclaredType) {
//      return testDeclaredType((IJavaDeclaredType) type, typeTester);
//    } else if (type instanceof IJavaArrayType) {
//      return typeTester.testArrayType((IJavaArrayType) type);
//    } else if (type instanceof IJavaCaptureType) {
//      final IJavaType upper = ((IJavaCaptureType) type).getUpperBound();
//      testType((upper == null) ? javaLangObject : upper, typeTester);
//    } else if (type instanceof IJavaIntersectionType) {
//      final IJavaIntersectionType intType = (IJavaIntersectionType) type;
//      final boolean first = testType(intType.getPrimarySupertype(), typeTester);
//      final boolean second = testType(intType.getSecondarySupertype(), typeTester);
//      /*
//       * Intersection implies AND, so you would think that we should conjoin
//       * the results below. But an interface that is not annotated with X may
//       * have X-annotated implementations. So mixing an unannotated interface
//       * into the intersection doesn't kill the possibility of X-ness. If the
//       * class portion of the intersection is not-X, then really the whole
//       * intersection should be false, but it's not possible to have a
//       * implementation that is X where the class is not-X and an interface is
//       * X (the sanity checking fails in this case), so it doesn't matter if we
//       * let this case through here.
//       */
//      return first || second;
//    } else if (type instanceof IJavaTypeFormal) {
//      // First check the formal against annotation bounds
//      final PromiseDrop<?> bound = 
//          typeTester.testFormalAgainstAnnotationBounds((IJavaTypeFormal) type);
//      if (bound != null) {
//        trusts.add(bound);
//        return true;
//      } else {
//        // Test the upperbound
//        final IJavaType upper = ((IJavaTypeFormal) type).getSuperclass(typeEnv);
//        return testType((upper == null) ? javaLangObject : upper, typeTester);
//      }
//    } else if (type instanceof IJavaUnionType) {
//      // Can't get the least upper bound, use object instead
//      return testType(javaLangObject, typeTester);
//    } else if (type instanceof IJavaWildcardType) {
//      // dead case?  Turned into Capture types, I think
//      final IJavaType upper = ((IJavaWildcardType) type).getUpperBound();
//      return testType((upper == null) ? javaLangObject : upper, typeTester);
//    } 
//    // shouldn't get here?
//    return false;
//  }
  
  
  
  private boolean testDeclaredType(final TypeTester typeTester,
      final IRNode typeDecl, final IRNode paramterizedType) {
    tested.add(typeDecl);
    final ProofDrop drop = typeTester.testTypeDeclaration(typeDecl);
    if (drop != null) {
      trusts.add(drop);
      if (paramterizedType != null) {
        final ResultFolderDrop annoBounds = annoBoundsFolders.get(paramterizedType);
        if (annoBounds != null) {
          trusts.add(annoBounds);
        }
      }
      return true;
    } else {
      failed.add(typeDecl);
      return false;
    }
  }  
  
  
//  private boolean testDeclaredType(final IJavaDeclaredType type, final TypeTester typeTester) {
//    final IRNode typeDecl = type.getDeclaration();
//    tested.add(typeDecl);
//    final ProofDrop drop = typeTester.testTypeDeclaration(typeDecl);
//    if (drop != null) {
//      trusts.add(drop);
//      final ResultFolderDrop annoBounds = annoBoundsFolders.get(type);
//      if (annoBounds != null) {
//        trusts.add(annoBounds);
//      }
//      return true;
//    } else {
//      failed.add(typeDecl);
//      return false;
//    }
//  }
}
