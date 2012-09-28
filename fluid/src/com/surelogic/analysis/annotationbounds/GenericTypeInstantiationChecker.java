package com.surelogic.analysis.annotationbounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.promise.AnnotationBoundsNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.type.constraints.ContainableAnnotationTester;
import com.surelogic.analysis.type.constraints.ITypeFormalEnv;
import com.surelogic.analysis.type.constraints.ImmutableAnnotationTester;
import com.surelogic.analysis.type.constraints.ReferenceObjectAnnotationTester;
import com.surelogic.analysis.type.constraints.ThreadSafeAnnotationTester;
import com.surelogic.analysis.type.constraints.TypeDeclAnnotationTester;
import com.surelogic.analysis.type.constraints.ValueObjectAnnotationTester;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.ProofDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.AnnotationBoundsPromiseDrop;

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
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Pair;

final class GenericTypeInstantiationChecker extends VoidTreeWalkVisitor implements IBinderClient {
  private static final int USE_CATEGORY = 495;
  
  private static final int ANNOTATION_BOUNDS_FOLDER = 495;
  private static final int ANNOTATION_BOUND_SATISFIED = 496;
  private static final int ANNOTATION_BOUND_NOT_SATISFIED = 497;
  private static final int USE = 498;
  
  
  
  private static final Map<IJavaType, ResultFolderDrop> folders =
      new ConcurrentHashMap<IJavaType, ResultFolderDrop>();
  private static final Map<IJavaType, ResultFolderDrop> foldersExternal =
      Collections.unmodifiableMap(folders);
  
  private static final Map<IRNode, List<Pair<IRNode, Set<AnnotationBounds>>>> cachedBounds =
      new ConcurrentHashMap<IRNode, List<Pair<IRNode, Set<AnnotationBounds>>>>();

  private final IBinder binder;
  private final ITypeFormalEnv formalEnv;

  
  
  private enum AnnotationBounds {
    CONTAINABLE {
      @Override
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode ast) {
        return ast.getContainable();
      }
      
      @Override
      public TypeDeclAnnotationTester getTester(
          final IBinder binder, final ITypeFormalEnv formalEnv) {
        return new ContainableAnnotationTester(binder, formalEnv, foldersExternal, false, false);
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
        return new ImmutableAnnotationTester(binder, formalEnv, foldersExternal, false, false);
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
        return new ReferenceObjectAnnotationTester(binder, formalEnv, foldersExternal, false);
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
        return new ThreadSafeAnnotationTester(binder, formalEnv, foldersExternal, false, false);
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
        return new ValueObjectAnnotationTester(binder, formalEnv, foldersExternal, false);
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
    binder = b;
    formalEnv = fe;
  }

  
  
  public static void clearCache() {
    cachedBounds.clear();
  }
  
  
  
  public IBinder getBinder() {
    return binder;
  }

  public void clearCaches() {
    // Nothing to do yet
  }
  
  
  
  static Map<IJavaType, ResultFolderDrop> getFolders() {
    return foldersExternal;
  }

  static void clearFolders() {
    folders.clear();
  }
  
  
  
  @Override
  public Void visitParameterizedType(final IRNode pType) {
    /* Need to go inside-out because if there are nested uses of parameterized
     * types, we may depend on the results of the nested uses, e.g.,
     * 
     *  CopyOnWriteArrayList<CopyOnWriteArrayList<X>>
     *  
     * The outer parameterization of CopyOnWriteArrayList depends on the 
     * correctness of the inner parameterization of CopyOnWriteArrayList.
     */
    doAcceptForChildren(pType);

    // See if there are any bounds to check
    final IRNode baseTypeDecl =
        binder.getBinding(ParameterizedType.getBase(pType));
    final AnnotationBoundsPromiseDrop boundsDrop = 
        LockRules.getAnnotationBounds(baseTypeDecl);
    if (boundsDrop != null) {
      /* Get the IJavaType and see if we already have a result for this
       * parameterization.  Should always get an IJavaDeclaredType.
       */
      final IJavaDeclaredType jTypeOfParameterizedType =
          (IJavaDeclaredType) binder.getJavaType(pType);
      ResultFolderDrop folder = folders.get(jTypeOfParameterizedType);
      if (folder == null) {
        final List<Pair<IRNode, Set<AnnotationBounds>>> bounds =
            getBounds(baseTypeDecl, boundsDrop);
        if (bounds != null) {
          folder = checkActualsAgainstBounds(pType, jTypeOfParameterizedType, bounds);
          folder.addChecked(boundsDrop);
          folders.put(jTypeOfParameterizedType, folder);
        }
      }
      // be safe
      if (folder != null) {
        folder.addInformationHintWithCategory(pType, USE_CATEGORY, USE);
      }
    }
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
  

  private ResultFolderDrop checkActualsAgainstBounds(
      final IRNode parameterizedType,
      final IJavaDeclaredType jTypeOfParameterizedType,
      final List<Pair<IRNode, Set<AnnotationBounds>>> boundsList) {    
    final List<IJavaType> actualList = jTypeOfParameterizedType.getTypeParameters();
    
    final ResultFolderDrop folder = ResultFolderDrop.newAndFolder(parameterizedType);
    folder.setMessage(
        ANNOTATION_BOUNDS_FOLDER, jTypeOfParameterizedType.toSourceText());
    for (int i = 0; i < boundsList.size(); i++) {
      final IRNode formalDecl = boundsList.get(i).first();
      final String nameOfTypeFormal = TypeFormal.getId(formalDecl);
      final Set<AnnotationBounds> bounds = boundsList.get(i).second();
      final String boundsString = boundsSetToString(bounds);
      final IJavaType jTypeOfActual = actualList.get(i);
      
      final Set<ProofDrop> trusts = new HashSet<ProofDrop>();
      boolean checks = false;
      for (final AnnotationBounds bound : bounds) {
        final TypeDeclAnnotationTester tester = bound.getTester(binder, formalEnv);
        checks |= tester.testType(jTypeOfActual);
        trusts.addAll(tester.getTrusts());
      }      

      final ResultDrop result = new ResultDrop(parameterizedType);
      result.setMessagesByJudgement(
          ANNOTATION_BOUND_SATISFIED, ANNOTATION_BOUND_NOT_SATISFIED,
          jTypeOfActual.toSourceText(), boundsString, nameOfTypeFormal);
      result.setConsistent(checks);
      result.addTrusted(trusts);
      folder.addTrusted(result);
    }
    return folder;
  }
}
