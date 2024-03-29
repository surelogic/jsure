package com.surelogic.analysis.concurrency.model;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import com.surelogic.aast.java.ClassExpressionNode;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.FieldRefNode;
import com.surelogic.aast.java.MethodCallNode;
import com.surelogic.aast.java.QualifiedThisExpressionNode;
import com.surelogic.aast.java.ThisExpressionNode;
import com.surelogic.aast.java.VariableUseExpressionNode;
import com.surelogic.aast.promise.AbstractLockDeclarationNode;
import com.surelogic.aast.promise.ClassLockExpressionNode;
import com.surelogic.aast.promise.ItselfNode;
import com.surelogic.aast.promise.LockDeclarationNode;
import com.surelogic.aast.promise.LockNameNode;
import com.surelogic.aast.promise.LockSpecificationNode;
import com.surelogic.aast.promise.LockType;
import com.surelogic.aast.promise.QualifiedLockNameNode;
import com.surelogic.aast.promise.SimpleLockNameNode;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.concurrency.model.declared.GuardedBy;
import com.surelogic.analysis.concurrency.model.declared.ModelLock;
import com.surelogic.analysis.concurrency.model.declared.PolicyLock;
import com.surelogic.analysis.concurrency.model.declared.RegionLock;
import com.surelogic.analysis.concurrency.model.declared.StateLock;
import com.surelogic.analysis.concurrency.model.implementation.ClassImplementation;
import com.surelogic.analysis.concurrency.model.implementation.FieldImplementation;
import com.surelogic.analysis.concurrency.model.implementation.LockImplementation;
import com.surelogic.analysis.concurrency.model.implementation.MethodImplementation;
import com.surelogic.analysis.concurrency.model.implementation.NamedLockImplementation;
import com.surelogic.analysis.concurrency.model.implementation.QualifiedThisImplementation;
import com.surelogic.analysis.concurrency.model.implementation.SelfImplementation;
import com.surelogic.analysis.concurrency.model.implementation.UnnamedLockImplementation;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLock;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLockFactory;
import com.surelogic.analysis.concurrency.model.instantiated.NeededLock;
import com.surelogic.analysis.concurrency.model.instantiated.NeededLockFactory;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.common.concurrent.ConcurrentHashSet;
import com.surelogic.common.util.AppendIterator;
import com.surelogic.common.util.EmptyIteratable;
import com.surelogic.common.util.FilterIterator;
import com.surelogic.common.util.Iteratable;
import com.surelogic.common.util.IteratorUtil;
import com.surelogic.common.util.SimpleIteratable;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.locks.GuardedByPromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.LockModel;
import com.surelogic.dropsea.ir.drops.locks.RequiresLockPromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.ReturnsLockPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaNullType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * Lock model for the entire set of classes being analyzed.  At its
 * core, it maps regions to locks.  This is subdivided by taking the class
 * hierarchy into consideration.  Also provides the means for looking up
 * locks by name or by implementing object reference.
 */
public final class AnalysisLockModel {
  /**
   * Name of the wait-queue lock defined for <code>java.lang.Object</code>
   * used by the {@link java.lang.Object#wait()}method, etc.
   */
  private static final String MUTEX_NAME = "MUTEX";
  
  
  
  /*
   * Map from Members->locks is used to go from lock expressions to locks.
   * This doesn't need the class hierarchy, except for filtering...
   * 
   * ...filtering of non-static locks is used so that 'c.f', where 'c'
   * is statically of type T, does not resolve to locks declared in subtypes of
   * T.  
   * 
   * Class hierarchy with lock declarations is used to determine what lock
   * is needed to protect a region.
   * 
   * Use Member to facilitate filtering...
   * 
   * 
   * Current GlobalLockModel has too many unused operations.  See which ones 
   * are really needed and only implement those.
   * 
   * 
   */
  
  /**
   * We map these to model locks.  Allows us to abstract away the difference
   * between using <code>this</code>, <code>.class</code>, a method, or a real
   * field as a lock.
   */
  private static interface Member {
    /**
     * Get the class model object of the class that declares the member.
     */
    public IJavaType getDeclaredInClass();
  }
  
  private abstract static class IRNodeMember implements Member {
    protected final IRNode decl;
    
    public IRNodeMember(final IRNode d) {
      decl = d;
    }
  }

  /**
   * Field used as a lock.
   */
  private final class Field extends IRNodeMember {
    // fieldDecl must be a VariableDeclarator from a FieldDeclaration
    public Field(final IRNode fieldDecl) {
      super(fieldDecl);
    }
    
    @Override
    public IJavaType getDeclaredInClass() {
      return binder.getTypeEnvironment().getMyThisType(
          VisitUtil.getEnclosingType(decl));
    }
    
    @Override
    public String toString() {
      return VariableDeclarator.getId(decl);
    }
    
    @Override
    public int hashCode() {
      return 31 * 17 + decl.hashCode();
    }
    
    @Override
    public boolean equals(final Object other) {
      if (other == this) { 
        return true;
      } else if (other instanceof Field) {
        return decl.equals(((Field) other).decl);
      } else {
        return false;
      }
    }
  }
  
  /**
   * Method used as a lock.
   */
  private final class Method extends IRNodeMember {
    // methodDecl must be a MethodDeclaration node 
    public Method(final IRNode methodDecl) {
      super(methodDecl);
    }
    
    @Override
    public IJavaType getDeclaredInClass() {
      return binder.getTypeEnvironment().getMyThisType(
          VisitUtil.getEnclosingType(decl));
    }
    
    @Override
    public String toString() {
      return MethodDeclaration.getId(decl) + "()";
    }
    
    @Override
    public int hashCode() {
      return 31 * 17 + decl.hashCode();
    }
    
    @Override
    public boolean equals(final Object other) {
      if (other == this) { 
        return true;
      } else if (other instanceof Method) {
        return decl.equals(((Method) other).decl);
      } else {
        return false;
      }
    }
  }
  
  /**
   * Class object used as a lock.
   */
  private final class ClassObject extends IRNodeMember {
    // typeDecl must be a TypeDeclaration
    public ClassObject(final IRNode typeDecl) {
      super(typeDecl);
    }
    
    @Override
    public IJavaType getDeclaredInClass() {
      return binder.getTypeEnvironment().getMyThisType(decl);
    }
    
    @Override
    public String toString() {
      return TypeDeclaration.getId(decl) + ".class";
    }
    
    @Override
    public int hashCode() {
      return 31 * 17 + decl.hashCode();
    }
    
    @Override
    public boolean equals(final Object other) {
      if (other == this) { 
        return true;
      } else if (other instanceof ClassObject) {
        return decl.equals(((ClassObject) other).decl);
      } else {
        return false;
      }
    }
  }
  
  /**
   * Qualified receiver used as lock
   */
  private final class QualifiedThis extends IRNodeMember {
    // typeDecl must be a TypeDeclaration
    public QualifiedThis(final IRNode typeDecl) {
      super(typeDecl);
    }
    
    // As in Self 
    @Override
    public IJavaType getDeclaredInClass() {
      return binder.getTypeEnvironment().getObjectType();
    }
    
    @Override
    public String toString() {
      return TypeDeclaration.getId(decl) + ".this";
    }
    
    @Override
    public int hashCode() {
      return 31 * 17 + decl.hashCode();
    }
    
    @Override
    public boolean equals(final Object other) {
      if (other == this) { 
        return true;
      } else if (other instanceof QualifiedThis) {
        return decl.equals(((QualifiedThis) other).decl);
      } else {
        return false;
      }
    }
  }
  
  /**
   * Receiver used as a lock.  There is only one of these, because we pretend
   * it is a "field" declared in the root class.
   * 
   * N.B. cannot make this an enum because we cannot be static: needs the 
   * binder reference from the lock model.
   */
  private final class Self implements Member {
    private Self() {
      super();
    }
    
    @Override
    public String toString() {
      return "this";
    }
    
    /**
     * Use default hash code and identity-based equality because we should only
     * ever have one of these in the model. 
     */
    
    @Override
    public IJavaType getDeclaredInClass() {
      return binder.getTypeEnvironment().getObjectType();
    }
  }
  
  // ======================================================================
  // ======================================================================
  
  private static final class Clazz {
    /**
     * Class record of the super class, or <code>null</code> if this
     * class is the root of the hierarchy.
     */
    private final Clazz parent;

    /** The type representation of the class. */
    private final IJavaType classDecl;    
    
    /**
     * The locks declared in this class.
     */
    /* XXX: Should this really contain PolicyLocks?  If not, then we should
     * replace the whole clazz hierarchy model wit ha map from regions to 
     * locks.  
     */
    private final Set<ModelLock<?, ?>> declaredLocks = new ConcurrentHashSet<>();

    
    
    public Clazz(final Clazz parent, final IJavaType decl) {
      this.parent = parent;
      this.classDecl = decl;
    }
    
    
    
    public void addLock(final ModelLock<?, ?> lock) {
      declaredLocks.add(lock);
    }
    
    public Iteratable<ModelLock<?, ?>> getDeclaredLocks() {
      return new SimpleIteratable<>(declaredLocks.iterator());
    }
    
    public Iteratable<StateLock<?, ?>> getDeclaredStateLocks() {
      return new FilterIterator<ModelLock<?, ?>, StateLock<?, ?>>(getDeclaredLocks()) {
        @Override
        protected Object select(final ModelLock<?, ?> lock) {
          if (lock instanceof StateLock) {
            return lock;
          } else {
            return IteratorUtil.noElement;
          }
          
        }
      };
    }
    
    // if includeMutex is true then the this better not be the class record for java.lang.Object
    public Iteratable<ModelLock<?, ?>> getAllLocksInClass(final boolean includeMutex) {
      // If !includeMutex then we skip the class record for java.lang.Object
      if ((includeMutex ? parent : parent.parent) == null) {
        return getDeclaredLocks();
      } else {
        return new AppendIterator<>(getDeclaredLocks(), parent.getAllLocksInClass(includeMutex));
      }
    }
    
    public Iteratable<StateLock<?, ?>> getAllStateLocksInClass() {
      return new FilterIterator<ModelLock<?, ?>, StateLock<?, ?>>(getAllLocksInClass(true)) {
        @Override
        protected Object select(final ModelLock<?, ?> lock) {
          if (lock instanceof StateLock) {
            return lock;
          } else {
            return IteratorUtil.noElement;
          }
          
        }
      };
    }
    
    public Clazz getParent() {
      return parent;
    }
   
    public void dumpClazz(final PrintWriter pw) {
      pw.println(classDecl.getName());
      for (final ModelLock<?, ?> lock : declaredLocks) {
        pw.print("  ");
        pw.println(lock);
      }
    }
  }
  
  // ====================================================================== 
  // ======================================================================
  
  private static abstract class SimpleExpressionNodeSwitch<T> {
    protected final IRNode annotatedItem;
    
    protected SimpleExpressionNodeSwitch(final IRNode annotatedItem) {
      this.annotatedItem = annotatedItem;
    }
    
    public final T apply(final ExpressionNode exprNode) {
      if (exprNode instanceof ClassExpressionNode) {
        return caseClassExpression((ClassExpressionNode) exprNode);
      } else if (exprNode instanceof ClassLockExpressionNode) { // XXX: Is this obsolete?
        return caseClassLockExpression((ClassLockExpressionNode) exprNode);
      } else if (exprNode instanceof FieldRefNode) {
        return caseFieldRef((FieldRefNode) exprNode);
      } else if (exprNode instanceof ItselfNode) {
        return caseItself((ItselfNode) exprNode);
      } else if (exprNode instanceof MethodCallNode) {
        return caseMethodCall((MethodCallNode) exprNode);
      } else if (exprNode instanceof QualifiedThisExpressionNode) {
        return caseQualifiedThisExpression((QualifiedThisExpressionNode) exprNode);
      } else if (exprNode instanceof ThisExpressionNode) {
        return caseThisExpression((ThisExpressionNode) exprNode);
      }
      return null;
    }
    
    protected abstract T caseClassExpression(ClassExpressionNode exprNode);
    protected abstract T caseClassLockExpression(ClassLockExpressionNode exprNode);
    protected abstract T caseFieldRef(FieldRefNode exprNode);
    protected abstract T caseItself(ItselfNode exprNode);
    protected abstract T caseMethodCall(MethodCallNode exprNode);
    protected abstract T caseQualifiedThisExpression(QualifiedThisExpressionNode exprNode);
    protected abstract T caseThisExpression(ThisExpressionNode exprNode);
  }

  private static final class ExpressionToLockImplSwitch
  extends SimpleExpressionNodeSwitch<UnnamedLockImplementation> {
    private ExpressionToLockImplSwitch(final IRNode annotatedItem) {
      super(annotatedItem);
    }
    
    @Override
    protected UnnamedLockImplementation caseClassExpression(final ClassExpressionNode exprNode) {
      return new ClassImplementation(
          (IJavaDeclaredType) exprNode.getType().resolveType().getJavaType());
    }
    
    @Override
    protected UnnamedLockImplementation caseClassLockExpression(final ClassLockExpressionNode exprNode) {
      return new ClassImplementation(
          (IJavaDeclaredType) exprNode.resolveType().getJavaType());
    }

    @Override
    protected UnnamedLockImplementation caseFieldRef(final FieldRefNode exprNode) {
      return new FieldImplementation(
          exprNode.resolveBinding().getNode(), false);
    }
  
    @Override
    protected UnnamedLockImplementation caseItself(final ItselfNode exprNode) {
      return new FieldImplementation(annotatedItem, true);
    }
  
    @Override
    protected UnnamedLockImplementation caseMethodCall(final MethodCallNode exprNode) {
      return new MethodImplementation(
          exprNode.resolveBinding().getNode());
    }
  
    @Override
    protected UnnamedLockImplementation caseQualifiedThisExpression(
        final QualifiedThisExpressionNode exprNode) {
      return new QualifiedThisImplementation(
          (IJavaDeclaredType) exprNode.resolveType().getJavaType());
    }

    @Override
    protected UnnamedLockImplementation caseThisExpression(final ThisExpressionNode exprNode) {
      return SelfImplementation.INSTANCE;
    }
  }

  private final class ExpressionToMemberSwitch
  extends SimpleExpressionNodeSwitch<Member> {
    private ExpressionToMemberSwitch(final IRNode annotatedItem) {
      super(annotatedItem);
    }
    
    @Override
    protected Member caseClassExpression(final ClassExpressionNode exprNode) {
      return new ClassObject(
          ((IJavaDeclaredType) exprNode.getType().resolveType().getJavaType()).getDeclaration());
    }
    
    @Override
    protected Member caseClassLockExpression(final ClassLockExpressionNode exprNode) {
      return new ClassObject(
          ((IJavaDeclaredType) exprNode.resolveType().getJavaType()).getDeclaration());
    }
  
    @Override
    protected Member caseFieldRef(final FieldRefNode exprNode) {
      return new Field(exprNode.resolveBinding().getNode());
    }
  
    @Override
    protected Member caseItself(final ItselfNode exprNode) {
      return new Field(annotatedItem);
    }
  
    @Override
    protected Member caseMethodCall(final MethodCallNode exprNode) {
      return new Method(exprNode.resolveBinding().getNode());
    }
    
    @Override
    protected Member caseQualifiedThisExpression(
        final QualifiedThisExpressionNode exprNode) {
      return new QualifiedThis(((IJavaDeclaredType) exprNode.getType().resolveType().getJavaType()).getDeclaration());
      
    }

    @Override
    protected Member caseThisExpression(final ThisExpressionNode exprNode) {
      return selfPrototype;
    }
  }
  
  // ======================================================================
  // ======================================================================
  
  /**
   * Multi-map from members to locks.  Main role is for use in converting
   * lock expressions to needed or held lock references.
   */
  @com.surelogic.GuardedBy("itself")
  private final SetMultimap<Member, ModelLock<?, ?>> membersToLocks =
      HashMultimap.create();
  
  /**
   * Map from binder types to Clazz objects.  Used for converting regions 
   * to needed locks.
   */
  @com.surelogic.GuardedBy("itself")
  private final Map<IJavaType, Clazz> classes = new HashMap<>();
  
  /**
   * Special member object used to represent the receiver throughout the model.
   */
  private final Self selfPrototype = new Self();

  /** The Binder. */
  private final IBinder binder;
  
  /**
   * The Clazz object for java.lang.Object
   */
  private final Clazz javaLangObject;

  // ----------------------------------------------------------------------
  
  public AnalysisLockModel(final IBinder binder) {
    this.binder = binder;
    this.javaLangObject = getClazzFor(binder.getTypeEnvironment().getObjectType());
  }

  private Clazz getClazzFor(final IRNode classDecl) {
    return getClazzFor(JavaTypeFactory.getMyThisType(classDecl));
  }
  
  private Clazz getClazzFor(final IJavaType javaType) {
    /* Need to use type erasure because type parameters muck
     * up the hierarchy.  In particular a parameterized subtype is shows up
     * as extending from Object instead of from the proper super class.
     */
    final ITypeEnvironment typeEnvironment = binder.getTypeEnvironment();
    final IJavaType erased = typeEnvironment.computeErasure(javaType);
    
    synchronized (classes) {
      Clazz clazz = classes.get(erased);
      if (clazz == null) {
        if (javaType instanceof IJavaNullType) { // getSuperclass doesn't work for this type
          clazz = new Clazz(null, erased); // lock record for the NULL type.  will always be empty
        } else {
          final IJavaType parent = javaType.getSuperclass(typeEnvironment);
          if (parent instanceof IJavaDeclaredType) {
            clazz = new Clazz(getClazzFor(parent),  erased);
          } else { // Should only be for java.lang.Object
            clazz = new Clazz(null, erased);
          }
        }
        classes.put(erased, clazz);
      }
      return clazz;
    }
  }
  
  // ----------------------------------------------------------------------

  public Iterable<ModelLock<?, ?>> getLocksImplemetedByClass(final IRNode cdecl) {
    synchronized(membersToLocks) {
      return membersToLocks.get(new ClassObject(cdecl));
    }
  }

  /**
   * Filter out locks declared in classes that are not direct ancestors of
   * the class of interest.  For example, assume we have field 'lock' declared in
   * class C.  Class C does NOT use 'lock' as a lock for anything.
   * Class D extends C and class E extends C.  In class D 'lock'
   * is used as a lock for region R and in class E the field 'lock' is used as
   * a lock for region Q.  If we have an expression "o.lock" and "lock" is the 
   * field declared in C, how we convert the expression to a lock depends on 
   * the static type of "o".  If "o" is C, then we the expression doesn't
   * convert to any locks.  If "o" is D, then it is the lock for region R, and
   * if "o" is E then it is the lock for region Q.  
   * 
   * @param classType The static type of the object expression.  Only locks
   * declared in classes that are ancestors of this class are returned.
   * 
   * @param locks The list of locks to filter.
   * 
   * @return
   */
  private Iterable<ModelLock<?, ?>> filterOutNonAncestorLocks(
      final IJavaType classType, final Set<ModelLock<?, ?>> locks) {
    return Iterables.filter(locks, new Predicate<ModelLock<?, ?>>() {
      @Override
      public boolean apply(final ModelLock<?, ?> input) {
        return classType.isSubtype(binder.getTypeEnvironment(), input.getDeclaredInClass());
      }
    });
  }
  
  // cdecl is the class that contains the method whose method is being analyzed
  public Iterable<ModelLock<?, ?>> getLocksImplementedByThis(final IJavaType classType) {
    final Set<ModelLock<?, ?>> locks;
    synchronized (membersToLocks) {
      locks = membersToLocks.get(selfPrototype);
    }
    return filterOutNonAncestorLocks(classType, locks);
  }
      
  public Iterable<ModelLock<?, ?>> getLocksImplementedByField(
      final IJavaType objType, final IRNode fieldDecl) {
    final Set<ModelLock<?, ?>> locks;
    synchronized (membersToLocks) {
      locks = membersToLocks.get(new Field(fieldDecl));
    }
    if (TypeUtil.isStatic(fieldDecl)) {
      // Static locks don't use filtering
      return locks;
    } else {
      return filterOutNonAncestorLocks(objType, locks);
    }
  }
  
  public Iterable<ModelLock<?, ?>> getLocksImplementedByMethod(
      final IJavaType objType, final IRNode mdecl) {
    final Set<ModelLock<?, ?>> locks;
    synchronized (membersToLocks) {
      locks = membersToLocks.get(new Method(mdecl));
    }
    if (TypeUtil.isStatic(mdecl)) {
      // Static locks don't use filtering
      return locks;
    } else {
      return filterOutNonAncestorLocks(objType, locks);
    }
  }
  
  public Iterable<ModelLock<?, ?>> getAllLocksImplementedByMethod(final IRNode mdecl) {
    synchronized (membersToLocks) {
      return membersToLocks.get(new Method(mdecl));
    }
  }
  
  // ----------------------------------------------------------------------

  private void insertLockIntoModel(final IRNode lockDeclaredInClassDecl,
      final Member memberUsedAsLock, final ModelLock<?, ?> lock) {
    synchronized (membersToLocks) {
      membersToLocks.put(memberUsedAsLock, lock);
    }
    final Clazz clazz = getClazzFor(lockDeclaredInClassDecl);
    clazz.addLock(lock);
  }
  
  public void addLockDeclaration(final LockModel lockDeclDrop) {
    final NamedLockImplementation namedLockImpl = getNamedLockImplementation(lockDeclDrop);
    if (namedLockImpl != null) {
      final AbstractLockDeclarationNode aastNode = lockDeclDrop.getAAST();
      final ExpressionNode lockField = aastNode.getField();
      final IRNode promisedFor = lockDeclDrop.getPromisedFor();
      final Member member = getMember(promisedFor, lockField);
      if (aastNode instanceof LockDeclarationNode) {
        insertLockIntoModel(promisedFor, member, new RegionLock(lockDeclDrop, namedLockImpl));
      } else { // PolicyLockDeclarationNode
        insertLockIntoModel(promisedFor, member, new PolicyLock(lockDeclDrop, namedLockImpl));
      }
    }
  }
  
  public void addGuardedByDelaration(final GuardedByPromiseDrop guardedByDrop) {
    final IRNode promisedFor = guardedByDrop.getPromisedFor();
    final ExpressionNode lockField = guardedByDrop.getAAST().getLock();
    final UnnamedLockImplementation lockImpl =
        getLockImplementation(promisedFor, lockField);
    if (lockImpl != null) {
      final Member member = getMember(promisedFor, lockField);
      insertLockIntoModel(VisitUtil.getEnclosingType(promisedFor),
          member, new GuardedBy(guardedByDrop, lockImpl));
    }
  }
  
  // ----------------------------------------------------------------------
  
  // returns null if the expression cannot be turned into a lock implementation
  private UnnamedLockImplementation getLockImplementation(
      final IRNode annotatedItem, final ExpressionNode exprNode) {
    return new ExpressionToLockImplSwitch(annotatedItem).apply(exprNode);
  }
  
  private NamedLockImplementation getNamedLockImplementation(final LockModel lockDeclDrop) {
    final AbstractLockDeclarationNode aastNode = lockDeclDrop.getAAST();
    final ExpressionNode lockField = aastNode.getField();
    final IRNode promisedFor = lockDeclDrop.getPromisedFor();
    final UnnamedLockImplementation baseLockImpl =
        getLockImplementation(promisedFor, lockField);
    if (baseLockImpl != null) {
      return new NamedLockImplementation(promisedFor, aastNode.getId(), baseLockImpl);
    } else {
      return null;
    }
  }
  
  // returns null if the expression cannot be turned into a lock implementation
  private Member getMember(
      final IRNode annotatedItem, final ExpressionNode exprNode) {
    return new ExpressionToMemberSwitch(annotatedItem).apply(exprNode);
  }
  
  // ----------------------------------------------------------------------
  
  public void dumpModel(final PrintWriter pw) {
    pw.println("LOCKS:");
    for (final Map.Entry<Member, Collection<ModelLock<?, ?>>> e : membersToLocks.asMap().entrySet()) {
      pw.println(e); // hopefully toString() is sane
    }
    pw.println();
    
    for (final Clazz clazz : classes.values()) {
      clazz.dumpClazz(pw);
    }
  }
  
  // ----------------------------------------------------------------------

  /**
   * Get the model lock for the lock that protects the given region in the 
   * given class.
   * @return The lock declaration for the lock that protects the given region,
   *         which may in fact be associated with a super region.
   *         <code>null</code> if the region is unprotected.
   */
  public StateLock<?, ?> getLockForRegion(
      final IJavaType javaType, final IRegion region) {
    Clazz currentClazz = getClazzFor(javaType);
    while (currentClazz != null) {
      for (final ModelLock<?, ?> lock : currentClazz.getDeclaredLocks()) {
        if (lock instanceof StateLock<?, ?>) {
          /* This only works because sanity checking already makes sure each 
           * region is protected by at most 1 lock.
           */
          final StateLock<?, ?> stateLock = (StateLock<?, ?>) lock;
          if (stateLock.protects(region)) {
            return stateLock;
          }
        }
      }
      currentClazz = currentClazz.getParent();
    }
    return null;
  }

  public StateLock<?, ?> getLockForFieldRef(final IRNode fieldRef) {
    return getLockForRegion(binder.getJavaType(FieldRef.getObject(fieldRef)),
        RegionModel.getInstance(binder.getBinding(fieldRef)));
  }
  
  public StateLock<?, ?> getLockForTarget(final IBinder binder, final Target target) {
    return getLockForRegion(target.getRelativeClass(binder), target.getRegion());
  }
  
  public Set<NeededLock> getNeededLocks(
      final NeededLockFactory lockFactory, 
      final IJavaType javaType, final IRegion region,
      final IRNode srcExpr, final NeededLock.Reason reason,
      final boolean needsWrite, final IRNode objectExpr) {
    return new LockInstantiator(
        lockFactory, getLockForRegion(javaType, region)).getLocks(
            srcExpr, reason, needsWrite, objectExpr);
  }
  
  public Set<NeededLock> getNeededLocks(
      final NeededLockFactory lockFactory, 
      final ThisExpressionBinder thisExprBinder, final Target target,
      final IRNode srcExpr, final NeededLock.Reason reason,
      final boolean needsWrite, final IRNode objectExpr) {
    return new LockInstantiator(
        lockFactory, getLockForTarget(thisExprBinder, target)).getLocks(
            srcExpr, reason, needsWrite, objectExpr);
  }
  
  private static final class LockInstantiator {
    private final NeededLockFactory lockFactory;
    private final StateLock<?, ?> stateLock;
    
    public LockInstantiator(final NeededLockFactory lockFactory, final StateLock<?, ?> stateLock) {
      this.lockFactory = lockFactory;
      this.stateLock = stateLock;
    }
    
    public Set<NeededLock> getLocks(
        final IRNode source, final NeededLock.Reason reason,
        final boolean needsWrite, final IRNode objectExpr) {
      if (stateLock == null) {
        return ImmutableSet.<NeededLock>of();
      } else {
        final NeededLock neededLock;
        if (stateLock.isStatic()) {
          neededLock = lockFactory.createStaticLock(
              stateLock.getImplementation(), source, reason,
              stateLock.getSourceAnnotation(), needsWrite);
        } else { // instance lock
          neededLock = lockFactory.createInstanceLock(
              objectExpr, stateLock.getImplementation(), source, reason,
              stateLock.getSourceAnnotation(), needsWrite);
        }
        return ImmutableSet.of(neededLock);
      }
    }
  }

  // ----------------------------------------------------------------------

  // Convert lock specification nodes to needed/held locks

  private abstract class LockSpecificationProcessor<T> {
    private final IRNode mdecl;
    private final IRNode source;
    
    
    
    protected LockSpecificationProcessor(
        final IRNode mdecl, final IRNode source) {
      this.mdecl = mdecl;
      this.source = source;
    }
    
    protected final T processLockSpecification(final LockSpecificationNode lockSpec) {
      final LockModel lockModel = lockSpec.resolveBinding().getModel();
      final NamedLockImplementation lockImpl =
          getNamedLockImplementation(lockModel);
      final boolean needsWrite = needsWrite(lockSpec);
      
      if (lockModel.isLockStatic()) {
        return createStaticLock(lockModel, lockImpl, source, needsWrite);
      } else {
        final LockNameNode lockName = lockSpec.getLock();
        final IRNode objExpr;
        if (lockName instanceof SimpleLockNameNode) {
          // Lock is "this.<LockName>"
          objExpr = JavaPromise.getReceiverNodeOrNull(mdecl);
        } else { // QualifiedLockNameNode
          final ExpressionNode base = ((QualifiedLockNameNode) lockName).getBase();
          if (base instanceof ThisExpressionNode) {
            // Lock is "this.<LockName>"
            objExpr = JavaPromise.getReceiverNodeOrNull(mdecl);
          } else if (base instanceof QualifiedThisExpressionNode) {
            // Lock is "Class.this.<LockName>"
            final QualifiedThisExpressionNode qthis = (QualifiedThisExpressionNode) base;
            objExpr = JavaPromise.getQualifiedReceiverNodeByName(
                mdecl, qthis.getType().resolveType().getNode());
          } else {
            VariableUseExpressionNode use = (VariableUseExpressionNode) base;
            final IRNode node = use.resolveBinding().getNode();
            if (VariableDeclarator.prototype.includes(node)) {
              /* Lock is "<field>.<LockName>".  Need to make into a field 
               * reference on the actual receiver.
               */
              return createFieldRefLock(
                  lockModel, JavaPromise.getReceiverNodeOrNull(mdecl), node, lockImpl, source, needsWrite);
            } else { // operator is ParameterDeclaration, find the actual
              // Lock is "<UseExpression>.<LockName>"
              // special case!
              return createInstanceParameterDeclLock(lockModel, node, lockImpl, source, needsWrite);
            }
          }
        }
        if (objExpr != null) {
          return createInstanceLock(lockModel, objExpr, lockImpl, source, needsWrite);
        } else {
          return null;
        }
      }
    }
    
    protected boolean needsWrite(final LockSpecificationNode lockSpec) {
      final LockType lockType = lockSpec.getType();
      return lockType != LockType.READ_LOCK;
    }
    
    protected abstract T createStaticLock(LockModel lockModel,
        LockImplementation lockImpl, IRNode source, boolean needsWrite);
    
    protected abstract T createInstanceLock(LockModel lockModel,
        IRNode objectRefExpr, LockImplementation lockImpl, IRNode source,
        boolean needsWrite);
    
    protected abstract T createFieldRefLock(LockModel lockModel,
        IRNode objectRefExpr, IRNode varDecl, LockImplementation lockImpl,
        IRNode source, boolean needsWrite);

    protected abstract T createInstanceParameterDeclLock(LockModel lockModel,
        IRNode paramDecl, LockImplementation lockImpl, IRNode source,
        boolean needsWrite);

//    protected abstract T createFieldRefLock(IRNode rcvr, IRNode fdecl, LockModel lock, boolean needsWrite);
  }
  
  private final class ReturnsLockProcessorHeldLocks
  extends LockSpecificationProcessor<HeldLock> {
    private HeldLock.Reason reason;
    private final Map<IRNode, IRNode> formalsToActuals; // or null, in the case of checking returns lock annotations
    private final HeldLockFactory heldLockFactory;
    private final boolean isWrite;
    
    protected ReturnsLockProcessorHeldLocks(
        final IRNode mdecl, final IRNode source, 
        final HeldLock.Reason reason, final boolean isWrite,
        final HeldLockFactory heldLockFactory,
        final Map<IRNode, IRNode> formalsToActuals) {
      super(mdecl, source);
      this.reason = reason;
      this.isWrite = isWrite;
      this.heldLockFactory = heldLockFactory;
      this.formalsToActuals = formalsToActuals;
    }
    
    @Override
    protected boolean needsWrite(final LockSpecificationNode lockSpec) {
      /* Returns lock annotation cannot name the read or write component
       * of the lock.  So when we convert a call to a lock method we get this 
       * based on the lock expression the call is a part of, for example,
       * "getLock().readLock()" where getLock() has the annotation.  So the
       * read/write flag is passed into this processor object and used here.
       */
      return isWrite;
    }
    
    public HeldLock processReturnedLock(final ReturnsLockPromiseDrop returnsLock) {
      return processLockSpecification(returnsLock.getAAST().getLock());
    }
    
    @Override
    protected HeldLock createStaticLock(final LockModel lockModel,
        final LockImplementation lockImpl, final IRNode source,
        final boolean needsWrite) {
      // XXX: supporting information drop should be the @RetunrsLock???  Old version doesn't so this so not sure why not
      return heldLockFactory.createStaticLock(
          lockImpl, source, reason, needsWrite, lockModel, null);
    }
    
    @Override
    protected HeldLock createInstanceLock(final LockModel lockModel,
        final IRNode objectRefExpr, final LockImplementation lockImpl,
        final IRNode source, final boolean needsWrite) {
      final IRNode mappedObjectExpr = 
          (formalsToActuals == null) ? objectRefExpr : formalsToActuals.get(objectRefExpr);
      if (mappedObjectExpr == null) {
        return null;
      } else {
        return heldLockFactory.createInstanceLock(
            mappedObjectExpr, lockImpl, source, reason, needsWrite, lockModel, null);
      }
    }
    
    @Override
    protected HeldLock createFieldRefLock(final LockModel lockModel,
        final IRNode objectRefExpr, final IRNode varDecl, final LockImplementation lockImpl,
        final IRNode source, final boolean needsWrite) {
      final IRNode mappedObjectExpr = 
          (formalsToActuals == null) ? objectRefExpr : formalsToActuals.get(objectRefExpr);
      if (mappedObjectExpr == null) {
        return null;
      } else {
        return heldLockFactory.createFieldRefLock(
            mappedObjectExpr, varDecl, lockImpl, source, reason, needsWrite, lockModel, null);
      }
    }
    
    @Override
    protected HeldLock createInstanceParameterDeclLock(final LockModel lockModel,
        final IRNode paramDecl, final LockImplementation lockImpl,
        final IRNode source, final boolean needsWrite) {
      if (formalsToActuals == null) {
        return heldLockFactory.createInstanceParameterDeclLock(
            paramDecl, lockImpl, source, reason, needsWrite, lockModel, null);
      } else {
        final IRNode mappedObjectExpr = formalsToActuals.get(paramDecl);
        if (mappedObjectExpr == null) {
          return null;
        } else {
          return heldLockFactory.createInstanceLock(
              mappedObjectExpr, lockImpl, source, reason, needsWrite, lockModel, null);
        }
      }
    }
  }
  
  private abstract class LockPreconditionProcessor<T> 
  extends LockSpecificationProcessor<T> {
    protected LockPreconditionProcessor(
        final IRNode mdecl, final IRNode source) {
      super(mdecl, source);
    }

    // Builders may be aliased to each other if only one set is being created
    public final void processRequiresLock(final RequiresLockPromiseDrop requiresLock) {
      for(final LockSpecificationNode ln : requiresLock.getAAST().getLockList()) {
        final T lock = processLockSpecification(ln);
        if (lock == null) {
          handleNullRequiredLock(ln);
        } else {
          addRequiredLock(lock);
        }
      }
    }
    
    protected void handleNullRequiredLock(final LockSpecificationNode lockNode) {
      // do nothing
    }
    
    protected abstract void addRequiredLock(T lock);
  }

  private final class PreconditionProcessorNeededLocks
  extends LockPreconditionProcessor<NeededLock> {
    private final RequiresLockPromiseDrop requiresLockDrop;
    private final NeededLockFactory lockFactory; 
    private final ImmutableSet.Builder<LockSpecificationNode> badLocks;
    private final Map<IRNode, IRNode> formalToActualMap;
    private final ImmutableSet.Builder<NeededLock> builder;
    
    public PreconditionProcessorNeededLocks(
        final RequiresLockPromiseDrop requiresLockDrop,
        final NeededLockFactory lockFactory, 
        final ImmutableSet.Builder<LockSpecificationNode> badLocks,
        final IRNode mdecl, final IRNode source,
        final ImmutableSet.Builder<NeededLock> builder,
        final Map<IRNode, IRNode> formalToActualMap) {
      super(mdecl, source);
      this.requiresLockDrop = requiresLockDrop;
      this.lockFactory = lockFactory;
      this.badLocks = badLocks;
      this.formalToActualMap = formalToActualMap;
      this.builder = builder;
    }
    
    @Override
    protected void handleNullRequiredLock(final LockSpecificationNode lockNode) {
      badLocks.add(lockNode);
    }
    
    @Override
    protected void addRequiredLock(final NeededLock lock) {
      builder.add(lock);
    }
    
    @Override
    protected NeededLock createStaticLock(final LockModel lockModel,
        final LockImplementation lockImpl, final IRNode source, 
        final boolean needsWrite) {
      return lockFactory.createStaticLock(lockImpl, source,
          NeededLock.Reason.LOCK_PRECONDITION, requiresLockDrop, needsWrite);
    }
    
    @Override
    protected NeededLock createInstanceLock(final LockModel lockModel,
        final IRNode objectRefExpr, final LockImplementation lockImpl,
        final IRNode source, final boolean needsWrite) {
      final IRNode mappedObjectExpr = formalToActualMap.get(objectRefExpr);
      if (mappedObjectExpr == null) {
        return null;
      } else {
        return lockFactory.createInstanceLock(mappedObjectExpr, lockImpl, source,
            NeededLock.Reason.LOCK_PRECONDITION, requiresLockDrop, needsWrite);
      }
    }
    
    @Override
    protected NeededLock createFieldRefLock(final LockModel lockModel,
        final IRNode objectRefExpr, final IRNode varDecl, final LockImplementation lockImpl,
        final IRNode source, final boolean needsWrite) {
      final IRNode mappedObjectExpr = formalToActualMap.get(objectRefExpr);
      if (mappedObjectExpr == null) {
        return null;
      } else {
        return lockFactory.createFieldRefLock(mappedObjectExpr, varDecl, lockImpl, source,
            NeededLock.Reason.LOCK_PRECONDITION, requiresLockDrop, needsWrite);
      }
    }
    
    @Override
    protected NeededLock createInstanceParameterDeclLock(
        final LockModel lockModel, final IRNode paramDecl,
        final LockImplementation lockImpl, final IRNode source,
        final boolean needsWrite) {
      final IRNode mappedObjectExpr = formalToActualMap.get(paramDecl);
      if (mappedObjectExpr == null) {
        return null;
      } else {
        return lockFactory.createInstanceLock(mappedObjectExpr, lockImpl, source,
            NeededLock.Reason.LOCK_PRECONDITION, requiresLockDrop, needsWrite);
      }
    }
  }

  private final class PreconditionProcessorHeldLocks
  extends LockPreconditionProcessor<HeldLock> {
    private final HeldLockFactory heldLockFactory;
    private final RequiresLockPromiseDrop supportingDrop;
    private final ImmutableSet.Builder<HeldLock> intrinsicBuilder;
    private final ImmutableSet.Builder<HeldLock> jucBuilder;
    
    public PreconditionProcessorHeldLocks(
        final IRNode mdecl, final IRNode source,
        final ImmutableSet.Builder<HeldLock> intrinsicBuilder,
        final ImmutableSet.Builder<HeldLock> jucBuilder,
        final HeldLockFactory heldLockFactory,
        final RequiresLockPromiseDrop supportingDrop) {
      super(mdecl, source);
      this.heldLockFactory = heldLockFactory;
      this.supportingDrop = supportingDrop;
      this.intrinsicBuilder = intrinsicBuilder;
      this.jucBuilder = jucBuilder;
    }
    
    @Override
    protected void addRequiredLock(final HeldLock lock) {
      (lock.isIntrinsic(binder) ? intrinsicBuilder : jucBuilder).add(lock);
    }
    
    @Override
    protected HeldLock createStaticLock(final LockModel lockModel,
        final LockImplementation lockImpl, final IRNode source,  boolean needsWrite) {
      return heldLockFactory.createStaticLock(
          lockImpl, source, HeldLock.Reason.METHOD_PRECONDITION,
          needsWrite, lockModel, supportingDrop);
    }
    
    @Override
    protected HeldLock createInstanceLock(final LockModel lockModel,
        final IRNode objectRefExpr, final LockImplementation lockImpl,
        final IRNode source, final boolean needsWrite) {
      return heldLockFactory.createInstanceLock(
          objectRefExpr, lockImpl, source, HeldLock.Reason.METHOD_PRECONDITION,
          needsWrite, lockModel, supportingDrop);
    }
    
    @Override
    protected HeldLock createFieldRefLock(final LockModel lockModel,
        final IRNode objectRefExpr, final IRNode varDecl, final LockImplementation lockImpl,
        final IRNode source, final boolean needsWrite) {
      return heldLockFactory.createFieldRefLock(
          objectRefExpr, varDecl, lockImpl, source, HeldLock.Reason.METHOD_PRECONDITION,
          needsWrite, lockModel, supportingDrop);
    }
    
    @Override
    protected HeldLock createInstanceParameterDeclLock(final LockModel lockModel,
        final IRNode paramDecl, final LockImplementation lockImpl,
        final IRNode source, final boolean needsWrite) {
      return heldLockFactory.createInstanceParameterDeclLock(
          paramDecl, lockImpl, source, HeldLock.Reason.METHOD_PRECONDITION,
          needsWrite, lockModel, supportingDrop);
    }
  }
  
  /**
   * Get the needed locks from a call site.
   */
  public Set<NeededLock> getNeededLocksFromRequiresLock(
      final NeededLockFactory lockFactory, 
      final RequiresLockPromiseDrop requiresLock, final IRNode mcall, 
      final Map<IRNode, IRNode> formalToActualMap,
      final ImmutableSet.Builder<LockSpecificationNode> badLocks) {
    if (requiresLock != null) {
      final ImmutableSet.Builder<NeededLock> builder = ImmutableSet.builder();
      final PreconditionProcessorNeededLocks p = 
          new PreconditionProcessorNeededLocks(
              requiresLock, lockFactory, badLocks,
              binder.getBinding(mcall), mcall, builder, formalToActualMap);
      p.processRequiresLock(requiresLock);
      return builder.build();
    } else {
      return ImmutableSet.of();
    }
  }
  
  /**
   * Given a method, what are the locks assumed to be held inside the method.
   */
  public void getHeldLocksFromRequiresLock(
      final RequiresLockPromiseDrop requiresLock, final IRNode mdecl,
      final ImmutableSet.Builder<HeldLock> intrinsicBuilder,
      final ImmutableSet.Builder<HeldLock> jucBuilder,
      final HeldLockFactory heldLockFactory) {
    if (requiresLock != null) {
      final PreconditionProcessorHeldLocks p = new PreconditionProcessorHeldLocks(
              mdecl, mdecl, intrinsicBuilder, jucBuilder, heldLockFactory, requiresLock);
      p.processRequiresLock(requiresLock);
    }
  }
  
  /**
   * Get all the locks assumed to be held by a constructor if it is single-threaded. 
   * Assumes that it has already been determined if the constructor can be
   * treated as single-threaded.
   */
  public void getHeldLocksFromSingleThreadedConstructor(
      final IRNode cdecl,
      final ImmutableSet.Builder<HeldLock> intrinsicBuilder,
      final ImmutableSet.Builder<HeldLock> jucBuilder,
      final HeldLockFactory heldLockFactory) {
    final IRNode classDecl = VisitUtil.getEnclosingType(cdecl);
    final Clazz clazz = getClazzFor(classDecl);
    final IRNode rcvr = JavaPromise.getReceiverNodeOrNull(cdecl);
    /*
     * Go through all the STATE locks in the class and pick out all the locks that
     * protect instance regions. Caveat: We exclude the lock MUTEX for Object
     * because we do not want to be able to verify wait() and notify() calls as
     * result of the @synchronized annotation.
     */
    for (final StateLock<?, ?> lock : clazz.getAllStateLocksInClass()) {
      if (!lock.isStatic()) { // Don't want static locks
        // check if "MUTEX"
        final LockImplementation lockImpl = lock.getImplementation();
        if (!(lockImpl instanceof NamedLockImplementation) ||
            !((NamedLockImplementation) lockImpl).getName().equals(MUTEX_NAME)) {
          final HeldLock heldLock = heldLockFactory.createInstanceLock(
              rcvr, lock, cdecl, HeldLock.Reason.SINGLE_THREADED, true, null);
          (lock.isIntrinsic(binder) ? intrinsicBuilder : jucBuilder).add(heldLock);
        }
      }
    }
  }
  
  /**
   * Get all the locks assumed to be held by a synchronized method.
   * Only intrinsic locks can be acquired this way.  Checks whether the method
   * is synchronized or not; contrast with {@link #getHeldLocksFromSingleThreadedConstructor)}.
   */
  public void getHeldLocksFromSynchronizedMethod(
      final IRNode mdecl,
      final ImmutableSet.Builder<HeldLock> intrinsicBuilder,
      final HeldLockFactory heldLockFactory) {
    // Is the method even synchronized?
    if (JavaNode.getModifier(mdecl, JavaNode.SYNCHRONIZED)) {
      final IRNode classDecl = VisitUtil.getEnclosingType(mdecl);
      if (TypeUtil.isStatic(mdecl)) {
        for (final ModelLock<?, ?> lock : getLocksImplemetedByClass(classDecl)) {
          intrinsicBuilder.add(heldLockFactory.createStaticLock(
              lock, mdecl, HeldLock.Reason.STATIC_SYNCHRONIZED_METHOD,
              true, null));
        }
      } else {
        for (final ModelLock<?, ?> lock : getLocksImplementedByThis(
            JavaTypeFactory.getMyThisType(classDecl))) {
          intrinsicBuilder.add(heldLockFactory.createInstanceLock(
              JavaPromise.getReceiverNodeOrNull(mdecl),
              lock, mdecl, HeldLock.Reason.SYNCHRONIZED_METHOD, true, null));
        }
      }
    }
  }
      
  /**
   * Get all the locks held by a class initialization block.  These are all
   * the static state locks declared in the class.
   */
  public void getHeldLocksFromClassInitialization(
      final IRNode classInitDecl,
      final ImmutableSet.Builder<HeldLock> intrinsicBuilder,
      final ImmutableSet.Builder<HeldLock> jucBuilder,
      final HeldLockFactory heldLockFactory) {
    final Clazz clazz = getClazzFor(JavaPromise.getPromisedFor(classInitDecl));
    for (final StateLock<?, ?> lock : clazz.getDeclaredStateLocks()) {
      if (lock.isStatic()) { // Only want static locks
        final HeldLock heldLock = heldLockFactory.createStaticLock(
            lock, classInitDecl, HeldLock.Reason.CLASS_INITIALIZATION, true,  null);
        (lock.isIntrinsic(binder) ? intrinsicBuilder : jucBuilder).add(heldLock);
      }
    }
  }    
  
  /**
   * Get the lock returned by a method with a ReturnsLock annotation as a 
   * HeldLock.
   */
  public HeldLock getHeldLockFromReturnsLock(
      final ReturnsLockPromiseDrop returnsLock, final IRNode methodDecl,
      final boolean mustBeWrite, final IRNode source, final HeldLock.Reason reason,
      final Map<IRNode, IRNode> formalsToActuals,
      final HeldLockFactory heldLockFactory) {
    if (returnsLock == null) {
      return null;
    } else {
      final ReturnsLockProcessorHeldLocks p = new ReturnsLockProcessorHeldLocks(
          methodDecl, source, reason, mustBeWrite, heldLockFactory, formalsToActuals);
      return p.processReturnedLock(returnsLock);
    }
  }
      
  /**
   * Get the Mutex lock declared in java.lang.Object.
   */
  public LockModel getJavaLangObjectMutex() {
    for (final ModelLock<?, ?> lock : javaLangObject.getDeclaredLocks()) {
      if (lock instanceof StateLock) {
        final RegionLock regionLock = (RegionLock) lock;
        if (regionLock.getName().equals(MUTEX_NAME)) {
          return regionLock.getSourceAnnotation();
        }
      }
    }
    return null; // Shouldn't happen unless the execution environment is messed up
  }
  
  /**
   * Does the given class declare any locks (outside of MUTEX)?
   */
  public boolean classDeclaresLocks(final IJavaType type) {
    /* Here we rely on the fact that MUTEX is declared in java.lang.Object
     * and that java.lang.Object doesn't declare any other locks.
     */
    final Clazz current = getClazzFor(type);
    if (current != javaLangObject) {
      return current.getAllLocksInClass(false).hasNext();
    } else {
      return false;
    }
  }
  
  public Iteratable<ModelLock<?, ?>> getAllDeclaredLocksIn(
      final IJavaType type, final boolean includeMutex) {
    final Clazz current = getClazzFor(type);
    if (current == javaLangObject && !includeMutex) {
      return EmptyIteratable.get();
    } else {
      return current.getAllLocksInClass(includeMutex);
    }
  }
  
  public Set<StateLock<?, ?>> getAllStateLocksIn(final IJavaType type) {
    final ImmutableSet.Builder<StateLock<?, ?>> builder = ImmutableSet.builder();
    for (final StateLock<?, ?> lock : getClazzFor(type).getAllStateLocksInClass()) {
      builder.add(lock);
    }
    return builder.build();
  }
}
