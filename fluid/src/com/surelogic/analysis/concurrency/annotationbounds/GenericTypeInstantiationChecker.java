package com.surelogic.analysis.concurrency.annotationbounds;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
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
import com.surelogic.analysis.concurrency.util.ReferenceObjectAnnotationTester;
import com.surelogic.analysis.concurrency.util.ThreadSafeAnnotationTester;
import com.surelogic.analysis.concurrency.util.TypeDeclAnnotationTester;
import com.surelogic.analysis.concurrency.util.ValueObjectAnnotationTester;
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterizedType;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.drops.promises.AnnotationBoundsPromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.ResultFolderDropBuilder;
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
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode ast) {
        return ast.getContainable();
      }
      
      @Override
      public TypeDeclAnnotationTester getTester(
          final IBinder binder, final ITypeFormalEnv formalEnv) {
        return new ContainableAnnotationTester(binder, formalEnv, false);
      }
      
      @Override
      public String toString() { return "Containable"; }
    },
    
    IMMUTABLE {
      @Override
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode ast) {
        return ast.getImmutable();
      }
      
      @Override
      public TypeDeclAnnotationTester getTester(
          final IBinder binder, final ITypeFormalEnv formalEnv) {
        return new ImmutableAnnotationTester(binder, formalEnv, false);
      }
      
      @Override
      public String toString() { return "Immutable"; }
    },
    
    REFERENCE {
      @Override
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode ast) {
        return ast.getReference();
      }
      
      @Override
      public TypeDeclAnnotationTester getTester(
          final IBinder binder, final ITypeFormalEnv formalEnv) {
        return new ReferenceObjectAnnotationTester(binder, formalEnv, false);
      }
      
      @Override
      public String toString() { return "ReferenceObject"; }
    },
    
    THREADSAFE {
      @Override
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode ast) {
        return ast.getThreadSafe();
      }
      
      @Override
      public TypeDeclAnnotationTester getTester(
          final IBinder binder, final ITypeFormalEnv formalEnv) {
        return new ThreadSafeAnnotationTester(binder, formalEnv, false);
      }
      
      @Override
      public String toString() { return "ThreadSafe"; }
    },
    
    VALUE {
      @Override
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode ast) {
        return ast.getValue();
      }
      
      @Override
      public TypeDeclAnnotationTester getTester(
          final IBinder binder, final ITypeFormalEnv formalEnv) {
        return new ValueObjectAnnotationTester(binder, formalEnv, false);
      }
      
      @Override
      public String toString() { return "ValueObject"; }
    };
    
    public abstract NamedTypeNode[] getNamedTypes(
        AnnotationBoundsNode ast);
    
    public abstract TypeDeclAnnotationTester getTester(
        IBinder binder, ITypeFormalEnv formalEnv);
    
    @Override
    public abstract String toString();
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
      
    /* If we get here, 'type' is a class or interface declaration with
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
    
    final AnnotationBoundsNode ast = boundsDrop.getAAST();
    boolean added = false;
    for (final AnnotationBounds e : AnnotationBounds.values()) {
      added |= addToBounds(bounds, formalIDs, e.getNamedTypes(ast), e);
    }
      
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


  private static String boundsSetToString(final Set<AnnotationBounds> set) {
    final StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (final AnnotationBounds bound : set) {
      if (!first) {
        sb.append(" || ");
      } else {
        first = false;
      }
      sb.append(bound.toString());
    }
    return sb.toString();
  }
  

  private void checkActualsAgainstBounds(
      final AnnotationBoundsPromiseDrop boundsDrop,
      final IRNode parameterizedType,
      final List<Pair<IRNode, Set<AnnotationBounds>>> boundsList) {    
    // Should be true: if not, why not?
    final IJavaDeclaredType jTypeOfParameterizedType =
        (IJavaDeclaredType) binder.getJavaType(parameterizedType);
    final List<IJavaType> actualList = jTypeOfParameterizedType.getTypeParameters();
    

    final ResultFolderDropBuilder folder = ResultFolderDropBuilder.create(analysis);
    analysis.setResultDependUponDrop(folder, parameterizedType);
    folder.setResultMessage(Messages.ANNOTATION_BOUNDS_FOLDER,
        jTypeOfParameterizedType.toSourceText());
    folder.addCheckedPromise(boundsDrop);
    
    for (int i = 0; i < boundsList.size(); i++) {
      final IRNode formalDecl = boundsList.get(i).first();
      final String nameOfTypeFormal = TypeFormal.getId(formalDecl);
      final Set<AnnotationBounds> bounds = boundsList.get(i).second();
      final String boundsString = boundsSetToString(bounds);
      final IJavaType jTypeOfActual = actualList.get(i);
      
      final Set<PromiseDrop<? extends IAASTRootNode>> promises = 
          new HashSet<PromiseDrop<? extends IAASTRootNode>>();
      boolean checks = false;
      for (final AnnotationBounds bound : bounds) {
        final TypeDeclAnnotationTester tester = bound.getTester(binder, formalEnv);
        checks |= tester.testType(jTypeOfActual);
        promises.addAll(tester.getPromises());
      }      

      final int msg = checks ? Messages.ANNOTATION_BOUND_SATISFIED
          : Messages.ANNOTATION_BOUND_NOT_SATISFIED;
      final ResultDrop result = new ResultDrop();
      analysis.setResultDependUponDrop(result, parameterizedType);
      result.setResultMessage(msg, jTypeOfActual.toSourceText(),
            boundsString, nameOfTypeFormal);
      result.setConsistent(checks);
      for (final PromiseDrop<? extends IAASTRootNode> p : promises) {
        result.addTrustedPromise(p);
      }
      
      folder.add(result);
    }
  }
}
