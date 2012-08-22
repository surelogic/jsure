package com.surelogic.analysis.concurrency.annotationbounds;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.promise.AnnotationBoundsNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.concurrency.driver.Messages;
import com.surelogic.analysis.concurrency.util.ITypeFormalEnv;
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaCaptureType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaIntersectionType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.bind.IJavaWildcardType;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterizedType;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.AnnotationBoundsPromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Pair;

public final class GenericTypeInstantiationChecker extends VoidTreeWalkVisitor {
  private final AbstractWholeIRAnalysis<? extends IBinderClient, ?> analysis;

  private final IBinder binder;
  private final ITypeFormalEnv formalEnv;
  private final IJavaDeclaredType javaLangObject;

  private final Map<IRNode, List<Pair<String, Set<AnnotationBounds>>>> cachedBounds =
      new HashMap<IRNode, List<Pair<String, Set<AnnotationBounds>>>>();

  
  
  private enum AnnotationBounds {
    CONTAINABLE {
      @Override
      public boolean test(final IRNode typeDecl) {
        return LockRules.isContainableType(typeDecl);
      }
    },
    
    IMMUTABLE {
      @Override
      public boolean test(final IRNode typeDecl) {
        return LockRules.isImmutableType(typeDecl);
      }
    },
    
    THREADSAFE {
      @Override
      public boolean test(final IRNode typeDecl) {
        return LockRules.getThreadSafeTypePromise(typeDecl) != null;
      }
    };
    
    public abstract boolean test(IRNode typeDecl);
  }

  
  
  
  public GenericTypeInstantiationChecker(
      final AbstractWholeIRAnalysis<? extends IBinderClient, ?> a,
      final IBinder b, final ITypeFormalEnv fe) {
    analysis = a;
    binder = b;
    formalEnv = fe;
    javaLangObject = b.getTypeEnvironment().getObjectType();
  }

  
  
  @Override
  public Void visitParameterizedType(final IRNode pType) {
    final IRNode baseTypeDecl =
        binder.getBinding(ParameterizedType.getBase(pType));
    final AnnotationBoundsPromiseDrop boundsDrop = 
        LockRules.getAnnotationBounds(baseTypeDecl);
    if (boundsDrop != null) {
      final List<Pair<String, Set<AnnotationBounds>>> bounds =
          getBounds(baseTypeDecl, boundsDrop);
      if (bounds != null) {
        checkActualsAgainstBounds(boundsDrop, pType, bounds);
      }
    }
    
    doAcceptForChildren(pType);
    return null;
  }


  
  private List<Pair<String, Set<AnnotationBounds>>> getBounds(
      final IRNode baseTypeDecl, final AnnotationBoundsPromiseDrop boundsDrop) {
    final Operator op = JJNode.tree.getOperator(baseTypeDecl);
    if (ClassDeclaration.prototype.includes(op)) {
      final List<Pair<String, Set<AnnotationBounds>>> bounds = cachedBounds.get(baseTypeDecl);
      if (bounds == null) {
        return computeBounds(baseTypeDecl, boundsDrop, ClassDeclaration.getTypes(baseTypeDecl));
      } else {
        return bounds;
      }
    } else if (InterfaceDeclaration.prototype.includes(op)) {
      final List<Pair<String, Set<AnnotationBounds>>> bounds = cachedBounds.get(baseTypeDecl);
      if (bounds == null) {
        return computeBounds(baseTypeDecl, boundsDrop, InterfaceDeclaration.getTypes(baseTypeDecl));
      } else {
        return bounds;
      }
    } else {
      return null;
    }
  }

  private List<Pair<String, Set<AnnotationBounds>>> computeBounds(
        final IRNode baseTypeDecl, final AnnotationBoundsPromiseDrop boundsDrop,
        final IRNode typeFormalsNode) {
    // Shouldn't happen, but be safe
    if (typeFormalsNode == null || !JJNode.tree.hasChildren(typeFormalsNode)) {
      cachedBounds.put(baseTypeDecl, null);
      return null;
    }
      
    /* If we get here we 'type' is a class or interface declaration with
     * at least 1 type formal.
     */
      
    final List<IRNode> formalDecls = JJNode.tree.childList(typeFormalsNode);
    final int numFormals = formalDecls.size();
    final String[] formalIDs = new String[numFormals];
    final List<Pair<String, Set<AnnotationBounds>>> bounds = 
        new ArrayList<Pair<String, Set<AnnotationBounds>>>(numFormals);
    for (int i = 0; i < numFormals; i++) {
      final String formalID = TypeFormal.getId(formalDecls.get(i));
      bounds.add(new Pair<String, Set<AnnotationBounds>>(
          formalID, EnumSet.noneOf(AnnotationBounds.class)));
      formalIDs[i] = formalID;
    }
    
    final AnnotationBoundsNode ast = boundsDrop.getAST();
    boolean added = false;
    added |= addToBounds(
        bounds, formalIDs, ast.getContainable(), AnnotationBounds.CONTAINABLE);
    added |= addToBounds(
        bounds, formalIDs, ast.getImmutable(), AnnotationBounds.IMMUTABLE);
    added |= addToBounds(
        bounds, formalIDs, ast.getThreadSafe(), AnnotationBounds.THREADSAFE);
      
    if (added) {
      cachedBounds.put(baseTypeDecl, bounds);
      return bounds;
    } else {
      cachedBounds.put(baseTypeDecl, null);
      return null;
    }
  }

  private boolean addToBounds(
      final List<Pair<String, Set<AnnotationBounds>>> bounds,
      final String[] formalIDs, final NamedTypeNode[] boundedNames,
      final AnnotationBounds bound) {
    boolean added = false;
    for (final NamedTypeNode name : boundedNames) {
      final String id = name.getType();
      for (int i = 0; i < formalIDs.length; i++) {
        if (formalIDs[i].equals(id)) {
          bounds.get(i).second().add(bound);
          added = true;
          break;
        }
      }
    }
    return added;
  }



  private void checkActualsAgainstBounds(
      final AnnotationBoundsPromiseDrop boundsDrop,
      final IRNode parameterizedType,
      final List<Pair<String, Set<AnnotationBounds>>> boundsList) {
    /* We create the result before we know whether it is assured or not, so
     * we have to set the result TYPE later too.  We use a dummy type 
     * initially.
     */
    final ResultDropBuilder result = ResultDropBuilder.create(analysis, "dummy");
    analysis.setResultDependUponDrop(result, parameterizedType);
    result.addCheckedPromise(boundsDrop);
    
    boolean checks = true;
    // Should be true: if not, why not?
    final IJavaDeclaredType jTypeOfParameterizedType =
        (IJavaDeclaredType) binder.getJavaType(parameterizedType);
    final List<IJavaType> actualList = jTypeOfParameterizedType.getTypeParameters();
    for (int i = 0; i < boundsList.size(); i++) {
      final IRNode syntaxNodeOfActual = JJNode.tree.getChild(parameterizedType, i);
      final String nameOfTypeFormal = boundsList.get(i).first();
      final Set<AnnotationBounds> bounds = boundsList.get(i).second();
      final IJavaType jTypeOfActual = actualList.get(i);
      final IRNode upperDeclaredTypeOfActual =
          convertToDeclaredType(jTypeOfActual).getDeclaration();
    
      for (final AnnotationBounds bound : bounds) {
        if (bound.test(upperDeclaredTypeOfActual)) {
          result.addSupportingInformation(
              syntaxNodeOfActual, Messages.BOUND_SATISFIED, 
              jTypeOfActual.getName(), bound.name(), nameOfTypeFormal); 
        } else {
          checks = false;
          result.addSupportingInformation(
              syntaxNodeOfActual, Messages.BOUND_NOT_SATISFIED, 
              jTypeOfActual.getName(), bound.name(), nameOfTypeFormal); 
        }
      }
    }
    
    if (checks) {
      result.setType(Messages.toString(Messages.ANNOTATION_BOUNDS_SATISFIED));
      result.setResultMessage(Messages.ANNOTATION_BOUNDS_SATISFIED,
          jTypeOfParameterizedType.getName());
    } else {
      result.setType(Messages.toString(Messages.ANNOTATION_BOUNDS_NOT_SATISFIED));
      result.setResultMessage(Messages.ANNOTATION_BOUNDS_NOT_SATISFIED,
          jTypeOfParameterizedType.getName());
    }
  }
  
  private IJavaDeclaredType convertToDeclaredType(IJavaType ty) {
    while (!(ty instanceof IJavaDeclaredType)) {
      if (ty instanceof IJavaCaptureType) {
        final IJavaType upper = ((IJavaCaptureType) ty).getUpperBound();
        ty = (upper == null) ? javaLangObject : upper;
      } else if (ty instanceof IJavaWildcardType) {
        // dead case?  Turned into Capture types, I think
        final IJavaType upper = ((IJavaWildcardType) ty).getUpperBound();
        ty = (upper == null) ? javaLangObject : upper;
      } else if (ty instanceof IJavaTypeFormal) {
        final IJavaType upper = ((IJavaTypeFormal) ty).getSuperclass(binder.getTypeEnvironment());
        ty = (upper == null) ? javaLangObject : upper;
      } else if (ty instanceof IJavaArrayType) {
        // Need to be smarter about this for containable arrays
        ty = javaLangObject;
      } else if (ty instanceof IJavaIntersectionType) {
        ty = javaLangObject;
      } else {
        throw new IllegalStateException("Unexpected type: " + ty);
      }
    }
    return (IJavaDeclaredType) ty;
  }
}
