package com.surelogic.analysis.concurrency.annotationbounds;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.promise.AnnotationBoundsNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.concurrency.driver.Messages;
import com.surelogic.analysis.concurrency.util.ContainableAnnotationTester;
import com.surelogic.analysis.concurrency.util.ITypeFormalEnv;
import com.surelogic.analysis.concurrency.util.ImmutableAnnotationTester;
import com.surelogic.analysis.concurrency.util.ThreadSafeAnnotationTester;
import com.surelogic.analysis.concurrency.util.TypeDeclAnnotationTester;
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterizedType;
import edu.cmu.cs.fluid.java.operator.TypeActuals;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.AnnotationBoundVirtualDrop;
import edu.cmu.cs.fluid.sea.drops.promises.AnnotationBoundsPromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Pair;

public final class GenericTypeInstantiationChecker extends VoidTreeWalkVisitor {
  private final AbstractWholeIRAnalysis<? extends IBinderClient, ?> analysis;

  private final IBinder binder;
  private final ITypeFormalEnv formalEnv;

  private final Map<IRNode, List<Pair<IRNode, Set<AnnotationBounds>>>> cachedBounds =
      new HashMap<IRNode, List<Pair<IRNode, Set<AnnotationBounds>>>>();

  
  
  private enum AnnotationBounds {
    CONTAINABLE {
      @Override
      public TypeDeclAnnotationTester getTester(
          final IBinder binder, final ITypeFormalEnv formalEnv) {
        return new ContainableAnnotationTester(binder, formalEnv);
      }
    },
    
    IMMUTABLE {
      @Override
      public TypeDeclAnnotationTester getTester(
          final IBinder binder, final ITypeFormalEnv formalEnv) {
        return new ImmutableAnnotationTester(binder, formalEnv);
      }
    },
    
    THREADSAFE {
      @Override
      public TypeDeclAnnotationTester getTester(
          final IBinder binder, final ITypeFormalEnv formalEnv) {
        return new ThreadSafeAnnotationTester(binder, formalEnv);
      }
    };
    
    public abstract TypeDeclAnnotationTester getTester(
        IBinder binder, ITypeFormalEnv formalEnv);
  }

  
  
  
  public GenericTypeInstantiationChecker(
      final AbstractWholeIRAnalysis<? extends IBinderClient, ?> a,
      final IBinder b, final ITypeFormalEnv fe) {
    analysis = a;
    binder = b;
    formalEnv = fe;
  }

  
  
  @Override
  public Void visitParameterizedType(final IRNode pType) {
    final IRNode baseTypeDecl =
        binder.getBinding(ParameterizedType.getBase(pType));
    final AnnotationBoundsPromiseDrop boundsDrop = 
        LockRules.getAnnotationBounds(baseTypeDecl);
    if (boundsDrop != null) {
      final List<Pair<IRNode, Set<AnnotationBounds>>> bounds =
          getBounds(baseTypeDecl, boundsDrop);
      if (bounds != null) {
        checkActualsAgainstBounds(boundsDrop, pType, bounds);
      }
    }
    
    doAcceptForChildren(pType);
    return null;
  }


  
  private List<Pair<IRNode, Set<AnnotationBounds>>> getBounds(
      final IRNode baseTypeDecl, final AnnotationBoundsPromiseDrop boundsDrop) {
    final Operator op = JJNode.tree.getOperator(baseTypeDecl);
    if (ClassDeclaration.prototype.includes(op)) {
      final List<Pair<IRNode, Set<AnnotationBounds>>> bounds = cachedBounds.get(baseTypeDecl);
      if (bounds == null) {
        return computeBounds(baseTypeDecl, boundsDrop, ClassDeclaration.getTypes(baseTypeDecl));
      } else {
        return bounds;
      }
    } else if (InterfaceDeclaration.prototype.includes(op)) {
      final List<Pair<IRNode, Set<AnnotationBounds>>> bounds = cachedBounds.get(baseTypeDecl);
      if (bounds == null) {
        return computeBounds(baseTypeDecl, boundsDrop, InterfaceDeclaration.getTypes(baseTypeDecl));
      } else {
        return bounds;
      }
    } else {
      return null;
    }
  }

  private List<Pair<IRNode, Set<AnnotationBounds>>> computeBounds(
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
    final List<Pair<IRNode, Set<AnnotationBounds>>> bounds = 
        new ArrayList<Pair<IRNode, Set<AnnotationBounds>>>(numFormals);
    for (int i = 0; i < numFormals; i++) {
      final IRNode formalDecl = formalDecls.get(i);
      bounds.add(new Pair<IRNode, Set<AnnotationBounds>>(
          formalDecl, EnumSet.noneOf(AnnotationBounds.class)));
      formalIDs[i] = TypeFormal.getId(formalDecls.get(i));
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
      final List<Pair<IRNode, Set<AnnotationBounds>>> bounds,
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
      final List<Pair<IRNode, Set<AnnotationBounds>>> boundsList) {
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
    final IRNode typeActuals = ParameterizedType.getArgs(parameterizedType);
    for (int i = 0; i < boundsList.size(); i++) {
      final IRNode formalDecl = boundsList.get(i).first();
      final String nameOfTypeFormal = TypeFormal.getId(formalDecl);
      final Set<AnnotationBounds> bounds = boundsList.get(i).second();

      final IJavaType jTypeOfActual = actualList.get(i);
      final IRNode syntaxNodeOfActual = TypeActuals.getType(typeActuals, i);

      for (final AnnotationBounds bound : bounds) {
        final AnnotationBoundVirtualDrop vDrop =
            new AnnotationBoundVirtualDrop(formalDecl, bound.name(), nameOfTypeFormal);
        result.addTrustedPromise(vDrop);
        
        final TypeDeclAnnotationTester tester = bound.getTester(binder, formalEnv);
        final ResultDropBuilder subResult;
        if (tester.testType(jTypeOfActual)) {
          subResult = ResultDropBuilder.create(
              analysis, Messages.toString(Messages.BOUND_SATISFIED));
          subResult.setResultMessage(Messages.BOUND_SATISFIED, jTypeOfActual.toString());
          subResult.setConsistent();
        } else {
          checks = false;
          subResult = ResultDropBuilder.create(
              analysis, Messages.toString(Messages.BOUND_NOT_SATISFIED));
          subResult.setResultMessage(Messages.BOUND_NOT_SATISFIED, jTypeOfActual.toString());
          subResult.setInconsistent();
        }
        
        analysis.setResultDependUponDrop(subResult, syntaxNodeOfActual);
        subResult.addCheckedPromise(vDrop);
        for (final PromiseDrop<? extends IAASTRootNode> p : tester.getPromises()) {
          subResult.addTrustedPromise(p);
        }
      }
    }
    
    final int msg = checks ? Messages.ANNOTATION_BOUNDS_SATISFIED
        : Messages.ANNOTATION_BOUNDS_NOT_SATISFIED;
    result.setType(Messages.toString(msg));
    result.setResultMessage(msg, jTypeOfParameterizedType.toString());
    result.setConsistent(checks);
  }
}
