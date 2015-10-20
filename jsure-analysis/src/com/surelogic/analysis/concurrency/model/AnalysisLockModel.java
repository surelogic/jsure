package com.surelogic.analysis.concurrency.model;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.FieldRefNode;
import com.surelogic.aast.java.MethodCallNode;
import com.surelogic.aast.java.QualifiedThisExpressionNode;
import com.surelogic.aast.java.ThisExpressionNode;
import com.surelogic.aast.promise.AbstractLockDeclarationNode;
import com.surelogic.aast.promise.ClassLockExpressionNode;
import com.surelogic.aast.promise.ItselfNode;
import com.surelogic.aast.promise.LockDeclarationNode;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.common.concurrent.ConcurrentHashSet;
import com.surelogic.dropsea.ir.drops.locks.GuardedByPromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * Lock model for the entire set of classes being analyzed.  At its
 * core, it maps regions to locks.  This is subdivided by taking the class
 * hierarchy into consideration.  Also provides the means for looking up
 * locks by name or by implementing object reference.
 */
public final class AnalysisLockModel {
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
   * Receiver used as a lock.  There is only one of these, because we pretend
   * it is a "field" declared in the root class.
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
    
    // Should this be Iterable?
    public Set<ModelLock<?, ?>> getDeclaredLocks() {
      return declaredLocks;
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
      if (exprNode instanceof ClassLockExpressionNode) {
        return caseClassLockExpression((ClassLockExpressionNode) exprNode);
      } else if (exprNode instanceof FieldRefNode) {
        return caseFieldRef((FieldRefNode) exprNode);
      } else if (exprNode instanceof ItselfNode) {
        return caseItself((ItselfNode) exprNode);
      } else if (exprNode instanceof MethodCallNode) {
        return caseMethodCall((MethodCallNode) exprNode);
      } else if (exprNode instanceof QualifiedThisExpressionNode) {
        // We don't handle these; the scrubber already put out a warning about this
        return null;
      } else if (exprNode instanceof ThisExpressionNode) {
        return caseThisExpression((ThisExpressionNode) exprNode);
      }
      return null;
    }
    
    protected abstract T caseClassLockExpression(ClassLockExpressionNode exprNode);
    protected abstract T caseFieldRef(FieldRefNode exprNode);
    protected abstract T caseItself(ItselfNode exprNode);
    protected abstract T caseMethodCall(MethodCallNode exprNode);
    protected abstract T caseThisExpression(ThisExpressionNode exprNode);
  }

  private static final class ExpressionToLockImplSwitch
  extends SimpleExpressionNodeSwitch<UnnamedLockImplementation> {
    private ExpressionToLockImplSwitch(final IRNode annotatedItem) {
      super(annotatedItem);
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
    protected UnnamedLockImplementation caseThisExpression(final ThisExpressionNode exprNode) {
      return new SelfImplementation();
    }
  }

  private final class ExpressionToMemberSwitch
  extends SimpleExpressionNodeSwitch<Member> {
    private ExpressionToMemberSwitch(final IRNode annotatedItem) {
      super(annotatedItem);
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

  // ----------------------------------------------------------------------
  
  public AnalysisLockModel(final IBinder binder) {
    this.binder = binder;
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
        final IJavaType parent = javaType.getSuperclass(typeEnvironment);
        if (parent instanceof IJavaDeclaredType) {
          clazz = new Clazz(getClazzFor(parent),  erased);
        } else { // Should only be for java.lang.Object
          clazz = new Clazz(null, erased);
        }
        classes.put(erased, clazz);
      }
      return clazz;
    }
  }
  
  // ----------------------------------------------------------------------

  private void insertLockIntoModel(final Member member, final ModelLock<?, ?> lock) {
    synchronized (membersToLocks) {
      membersToLocks.put(member, lock);
    }
    final Clazz clazz = getClazzFor(member.getDeclaredInClass());
    clazz.addLock(lock);
  }
  
  public void addLockDeclaration(final LockModel lockDeclDrop) {
    final AbstractLockDeclarationNode aastNode = lockDeclDrop.getAAST();
    final ExpressionNode lockField = aastNode.getField();
    final IRNode promisedFor = lockDeclDrop.getPromisedFor();
    final UnnamedLockImplementation baseLockImpl = getLockImplementation(
            promisedFor, lockField);
    if (baseLockImpl != null) {
      final NamedLockImplementation namedLockImpl =
          new NamedLockImplementation(aastNode.getId(), baseLockImpl);
      final Member member = getMember(promisedFor, lockField);      
      if (aastNode instanceof LockDeclarationNode) {
        insertLockIntoModel(member, new RegionLock(lockDeclDrop, namedLockImpl));
      } else { // PolicyLockDeclarationNode
        insertLockIntoModel(member, new PolicyLock(lockDeclDrop, namedLockImpl));
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
      insertLockIntoModel(member, new GuardedBy(guardedByDrop, lockImpl));
    }
  }
  
  // ----------------------------------------------------------------------
  
  // returns null if the expression cannot be turned into a lock implementation
  private UnnamedLockImplementation getLockImplementation(
      final IRNode annotatedItem, final ExpressionNode exprNode) {
    return new ExpressionToLockImplSwitch(annotatedItem).apply(exprNode);
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
  // XXX: Do we need this 
  public StateLock<?, ?> getLockForRegion(
      final IJavaType javaType, final IRegion region) {
    final Clazz clazz = classes.get(javaType);
    if (clazz == null) {
      throw new IllegalArgumentException("Class " + javaType.getName() + " not found in the lock model");
    } else {
      for (final ModelLock<?, ?> lock : clazz.getDeclaredLocks()) {
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
      return null;
    }
  }

  
  // XXX: Do we need this 
  public StateLock<?, ?> getLockForTarget(final IBinder binder, final Target target) {
    return getLockForRegion(target.getRelativeClass(binder), target.getRegion());
  }
  
  public LockGenerator getLockGenerator(final IJavaType javaType, final IRegion region) {
    return new LockGenerator(getLockForRegion(javaType, region));
  }
  
  public LockGenerator getLockGenerator(final IBinder binder, final Target target) {
    return new LockGenerator(getLockForTarget(binder, target));
  }
  
  public NeededLock getNeededLock(final IJavaType javaType, final IRegion region,
      final IRNode srcExpr, final IRNode objectExpr) {
    return getLockGenerator(javaType, region).getLock(srcExpr, objectExpr);
  }
  
  public NeededLock getNeededLock(final IBinder binder, final Target target,
      final IRNode srcExpr, final IRNode objectExpr) {
    return getLockGenerator(binder, target).getLock(srcExpr, objectExpr);
  }
  
  public static final class LockGenerator {
    private final StateLock<?, ?> rawLock;
    
    public LockGenerator(final StateLock<?, ?> rawLock) {
      this.rawLock = rawLock;
    }
    
    public NeededLock getLock(final IRNode source, final IRNode objectExpr) {
      if (rawLock == null) {
        return new NeedsNoLock(source);
      } else if (rawLock.isStatic()) {
        return new NeededStaticLock(rawLock, source);
      } else { // instance lock
        return new NeededInstanceLock(objectExpr, rawLock, source);
      }
    }
  }
}
