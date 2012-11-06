package com.surelogic.analysis.annotationbounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.promise.AnnotationBoundsNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.type.constraints.TypeAnnotationTester;
import com.surelogic.analysis.type.constraints.TypeAnnotations;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.common.Pair;
import com.surelogic.dropsea.IKeyValue;
import com.surelogic.dropsea.KeyValueUtility;
import com.surelogic.dropsea.ir.ProofDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.AnnotationBoundsPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ContainablePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaSourceRefType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.CastExpression;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.DeclStatement;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterizedType;
import edu.cmu.cs.fluid.java.operator.TypeActuals;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

final class GenericTypeInstantiationChecker extends VoidTreeWalkVisitor implements IBinderClient {
  private static final int USE_CATEGORY = 495;
  
  private static final int ANNOTATION_BOUNDS_FOLDER = 495;
  private static final int ANNOTATION_BOUND_SATISFIED = 496;
  private static final int ANNOTATION_BOUND_NOT_SATISFIED = 497;
  private static final int USE = 498;
  private static final int TYPE_FORMAL_INFO = 499;
  private static final int ACTUAL_UNBOUNDED = 551;
  private static final int ACTUAL_ANNOTATED = 552;
  
  
  
  private static final Map<IJavaType, ResultFolderDrop> folders =
      new ConcurrentHashMap<IJavaType, ResultFolderDrop>();
  private static final Map<IJavaType, ResultFolderDrop> foldersExternal =
      Collections.unmodifiableMap(folders);
  
  // these two fields are only for classes with @AnnotationBounds
  private static final Set<IRNode> classesWithBounds = new HashSet<IRNode>();
  private static final Set<IRNode> instantiatedBoundClasses = new HashSet<IRNode>();
  
  private static final Map<IRNode, List<Pair<IRNode, Set<AnnotationBounds>>>> cachedBounds =
      new ConcurrentHashMap<IRNode, List<Pair<IRNode, Set<AnnotationBounds>>>>();

  private final IBinder binder;

  
  
  private enum AnnotationBounds {
    CONTAINABLE {
      @Override
      public NamedTypeNode[] getNamedTypes(final AnnotationBoundsNode ast) {
        return ast.getContainable();
      }
      
      @Override
      public void testType(final IJavaType type,
          final Map<AnnotationBounds, Set<ProofDrop>> actualAnnos,
          final IBinder binder) {
        testType(type, actualAnnos, binder, TypeAnnotations.CONTAINABLE, CONTAINABLE);
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
      public void testType(final IJavaType type,
          final Map<AnnotationBounds, Set<ProofDrop>> actualAnnos,
          final IBinder binder) {
        testType(type, actualAnnos, binder, TypeAnnotations.IMMUTABLE, IMMUTABLE);
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
      public void testType(final IJavaType type,
          final Map<AnnotationBounds, Set<ProofDrop>> actualAnnos,
          final IBinder binder) {
        testType(type, actualAnnos, binder, TypeAnnotations.REFERENCE_OBJECT, REFERENCE);
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
      public void testType(final IJavaType type,
          final Map<AnnotationBounds, Set<ProofDrop>> actualAnnos,
          final IBinder binder) {
        testType(type, actualAnnos, binder, TypeAnnotations.THREAD_SAFE, THREADSAFE);
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
      public void testType(final IJavaType type,
          final Map<AnnotationBounds, Set<ProofDrop>> actualAnnos,
          final IBinder binder) {
        testType(type, actualAnnos, binder, TypeAnnotations.VALUE_OBJECT, VALUE);
      }
      
      @Override
      public String toString() { return "ValueObject"; }
    };
    
    public abstract NamedTypeNode[] getNamedTypes(
        AnnotationBoundsNode ast);
    
    public abstract void testType(final IJavaType type,
        final Map<AnnotationBounds, Set<ProofDrop>> actualAnnos,
        final IBinder binder);
    
    protected static void testType(
        final IJavaType type, final Map<AnnotationBounds,
        Set<ProofDrop>> actualAnnos, final IBinder binder,
        final TypeAnnotations typeAnnoToTestFor, final AnnotationBounds bound) {
      final TypeAnnotationTester tester =
          new TypeAnnotationTester(typeAnnoToTestFor, binder, foldersExternal);
      if (tester.testParameterizedTypeActual(type)) {
        actualAnnos.put(bound, tester.getTrusts());
      }
    }
    
    @Override
    public abstract String toString();
  }

  
  public GenericTypeInstantiationChecker(
      final AbstractWholeIRAnalysis<? extends IBinderClient, ?> a,
      final IBinder b) {
    binder = b;
  }

  
  
  public static void clearCache() {
    cachedBounds.clear();
  }
  
  
  
  @Override
  public IBinder getBinder() {
    return binder;
  }

  @Override
  public void clearCaches() {
    // Nothing to do yet
  }
  
  
  
  static Set<IRNode> getUnusedBoundClasses() {
    final Set<IRNode> unused = new HashSet<IRNode>();
    for (final IRNode cdecl : classesWithBounds) {
      if (!instantiatedBoundClasses.contains(cdecl)) unused.add(cdecl);
    }
    return unused;
  }
  
  static Map<IJavaType, ResultFolderDrop> getFolders() {
    return foldersExternal;
  }

  static void clearStaticState() {
    folders.clear();
    classesWithBounds.clear();
    instantiatedBoundClasses.clear();
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
    final ContainablePromiseDrop containableDrop =
        LockRules.getContainableImplementation(baseTypeDecl);
    
    if (boundsDrop != null || containableDrop != null) {
      /* Get the IJavaType and see if we already have a result for this
       * parameterization.  Should always get an IJavaDeclaredType.
       */
      final IJavaDeclaredType jTypeOfParameterizedType =
          (IJavaDeclaredType) binder.getJavaType(pType);
      ResultFolderDrop folder = folders.get(jTypeOfParameterizedType);
      if (folder == null) {
        final List<Pair<IRNode, Set<AnnotationBounds>>> bounds =
            getBounds(baseTypeDecl, boundsDrop, containableDrop, true);
        if (bounds != null) {
          folder = checkActualsAgainstBounds(pType, jTypeOfParameterizedType, bounds);
          /* Don't add the folder to the top-level if the only bounds come
           * implicitly from @Containable
           */
          if (boundsDrop != null) folder.addChecked(boundsDrop);
          
          folders.put(jTypeOfParameterizedType, folder);
        }
      }
      /* Don't add the "USE" link if the bounds are only implicit bounds
       * from @Containable. 
       */
      if (folder != null && boundsDrop != null) {
        folder.addInformationHintWithCategory(
            pType, USE_CATEGORY, USE, getUseContext(pType));
      }
    }
    return null;
  }


  
  private List<Pair<IRNode, Set<AnnotationBounds>>> getBounds(
      final IRNode baseTypeDecl, final AnnotationBoundsPromiseDrop boundsDrop,
      final ContainablePromiseDrop containableDrop,
      final boolean isForParameterizedUse) {
    if (boundsDrop != null && isForParameterizedUse) {
      instantiatedBoundClasses.add(baseTypeDecl);
    }
    
    final Operator op = JJNode.tree.getOperator(baseTypeDecl);
    if (ClassDeclaration.prototype.includes(op)) {
      final List<Pair<IRNode, Set<AnnotationBounds>>> bounds = cachedBounds.get(baseTypeDecl);
      if (bounds == null) {
        return computeBounds(baseTypeDecl, boundsDrop, containableDrop,
            ClassDeclaration.getTypes(baseTypeDecl));
      } else {
        return bounds;
      }
    } else if (InterfaceDeclaration.prototype.includes(op)) {
      final List<Pair<IRNode, Set<AnnotationBounds>>> bounds = cachedBounds.get(baseTypeDecl);
      if (bounds == null) {
        return computeBounds(baseTypeDecl, boundsDrop, containableDrop, 
            InterfaceDeclaration.getTypes(baseTypeDecl));
      } else {
        return bounds;
      }
    } else {
      return null;
    }
  }

  private List<Pair<IRNode, Set<AnnotationBounds>>> computeBounds(
        final IRNode baseTypeDecl, final AnnotationBoundsPromiseDrop boundsDrop,
        final ContainablePromiseDrop containableDrop,
        final IRNode typeFormalsNode) {
    // Shouldn't happen, but be safe
    if (typeFormalsNode == null || !JJNode.tree.hasChildren(typeFormalsNode)) {
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
      formalIDs[i] = TypeFormal.getId(formalDecl);
    }
    
    boolean added = false;
    if (boundsDrop != null) {
      final AnnotationBoundsNode ast = boundsDrop.getAAST();
      for (final AnnotationBounds e : AnnotationBounds.values()) {
        added |= addToBounds(bounds, formalIDs, e.getNamedTypes(ast), e);
      }
    }
    if (containableDrop != null) {
      final boolean allowRef = containableDrop.allowReferenceObject();
      // Add @ThreadSafe to all the formal parameters
      for (final Pair<IRNode, Set<AnnotationBounds>> pair : bounds) {
        final Set<AnnotationBounds> boundsSet = pair.second();
        boundsSet.add(AnnotationBounds.THREADSAFE);
        if (allowRef) boundsSet.add(AnnotationBounds.REFERENCE);
      }
      added = true;
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


  private static String boundsSetToString(
      final Set<AnnotationBounds> set, final String seperator) {
    final StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (final AnnotationBounds bound : set) {
      if (!first) {
        sb.append(seperator);
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
    final IRNode typeActuals = ParameterizedType.getArgs(parameterizedType);
    
    final ResultFolderDrop folder = ResultFolderDrop.newAndFolder(parameterizedType);
    folder.setIgnoreJavaRef(true);
    folder.setMessage(
        ANNOTATION_BOUNDS_FOLDER, jTypeOfParameterizedType.toSourceText());
    for (int i = 0; i < boundsList.size(); i++) {
      final IRNode typeActual = TypeActuals.getType(typeActuals, i);
      final IRNode formalDecl = boundsList.get(i).first();
      final String nameOfTypeFormal = TypeFormal.getId(formalDecl);
      final Set<AnnotationBounds> bounds = boundsList.get(i).second();
      // perhaps the bounds are empty for this type formal
      if (!bounds.isEmpty()) {
        final String boundsString = boundsSetToString(bounds, " || ");
        final IJavaType jTypeOfActual = actualList.get(i);
        
        final ResultFolderDrop actualFolder = ResultsBuilder.createAndFolder(
            folder, typeActual,
            ANNOTATION_BOUND_SATISFIED, ANNOTATION_BOUND_NOT_SATISFIED,
            jTypeOfActual.toSourceText(), boundsString, nameOfTypeFormal);
        
        /* The actual must be annotated with at least one of the type 
         * annotations required by the type formal, and cannot be annotated
         * with any type annotation not required by the type formal.  That is,
         * the type annotations on the actual must be a non-empty subset of
         * those required by the type formal.  So we actually have to test 
         * the type formal against all possible type annotations.
         */
        final Map<AnnotationBounds, Set<ProofDrop>> actualAnnos =
            new HashMap<AnnotationBounds, Set<ProofDrop>>();
        for (final AnnotationBounds ab : AnnotationBounds.values()) {
          ab.testType(jTypeOfActual, actualAnnos, binder);
        }
        
        final IRNode link = jTypeOfActual instanceof IJavaSourceRefType ?
            ((IJavaSourceRefType) jTypeOfActual).getDeclaration() : typeActual;
        final IKeyValue diffInfo = KeyValueUtility.getIntInstance(
            "actual type parameter", typeActual.hashCode());
        if (actualAnnos.isEmpty()) {
          final ResultDrop r = ResultsBuilder.createResult(
              false, actualFolder, link, ACTUAL_UNBOUNDED);
          r.addOrReplaceDiffInfo(diffInfo);
        } else {
          for (final Map.Entry<AnnotationBounds, Set<ProofDrop>> aa : actualAnnos.entrySet()) {
            final AnnotationBounds key = aa.getKey();
            boolean isConsistent = bounds.contains(key);
            // handle the fact that @Immutable can be given to @ThreadSafe
            if (!isConsistent && key == AnnotationBounds.IMMUTABLE) {
              isConsistent = bounds.contains(AnnotationBounds.THREADSAFE);
            }
            final ResultDrop result = ResultsBuilder.createResult(
                isConsistent, actualFolder, link,
                ACTUAL_ANNOTATED, key.toString());
            result.addTrusted(aa.getValue());
            result.addOrReplaceDiffInfo(diffInfo);
          }
        }
      }
    }
    return folder;
  }
  
  @Override
  public Void visitClassDeclaration(final IRNode cdecl) {
    processTypeDeclaration(cdecl, ClassDeclaration.getTypes(cdecl));
    return super.visitClassDeclaration(cdecl);
  }
  
  @Override
  public Void visitInterfaceDeclaration(final IRNode idecl) {
    processTypeDeclaration(idecl, InterfaceDeclaration.getTypes(idecl));
    return super.visitInterfaceDeclaration(idecl);
  }

  private void processTypeDeclaration(final IRNode cdecl, final IRNode formals) {
    if (formals != null && JJNode.tree.hasChildren(formals)) {
      final AnnotationBoundsPromiseDrop boundsDrop = 
          LockRules.getAnnotationBounds(cdecl);
      final ContainablePromiseDrop cDrop = 
          LockRules.getContainableImplementation(cdecl);
      if (boundsDrop != null || cDrop != null) {
        final List<Pair<IRNode, Set<AnnotationBounds>>> boundsPairs =
            getBounds(cdecl, boundsDrop, cDrop, false);
        if (boundsPairs != null) {
          // if we have both drops, put on both (shouldn't happen often)
          for (final Pair<IRNode, Set<AnnotationBounds>> pair : boundsPairs) {
            final Set<AnnotationBounds> bounds = pair.second();
            if (!bounds.isEmpty()) {
              final String boundsString = boundsSetToString(bounds, " or ");
              final IRNode typeFormal = pair.first();
              final String typeFormalName = TypeFormal.getId(typeFormal);
              if (boundsDrop != null) {
                boundsDrop.addInformationHint(
                    typeFormal, TYPE_FORMAL_INFO, typeFormalName, boundsString);
              }
              if (cDrop != null) {
                cDrop.addInformationHint(
                    typeFormal, TYPE_FORMAL_INFO, typeFormalName, boundsString);
              }
            }
          }
        }
      }
    
      if (boundsDrop != null) classesWithBounds.add(cdecl);
    }
  }
  
  private static String getUseContext(final IRNode pType) {
    final Operator op = JJNode.tree.getOperator(JJNode.tree.getParent(pType));
    if (FieldDeclaration.prototype.includes(op)) {
      return "field type";
    } else if (NewExpression.prototype.includes(op)) {
      return "new expression";
    } else if (MethodDeclaration.prototype.includes(op)) {
      return "method return type";
    } else if (ParameterDeclaration.prototype.includes(op)) {
      return "parameter type";
    } else if (DeclStatement.prototype.includes(op)) {
      return "local variable type";
    } else if (CastExpression.prototype.includes(op)) {
      return "type cast";
    } else if (TypeActuals.prototype.includes(op)) {
      return "type actual";
    }
    return "other";
  }
}
