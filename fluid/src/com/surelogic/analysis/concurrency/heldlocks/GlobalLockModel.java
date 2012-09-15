/* Created on Mar 18, 2005
 */
package com.surelogic.analysis.concurrency.heldlocks;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.aast.java.*;
import com.surelogic.aast.promise.*;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;

/**
 * TODO: Write javadoc!
 * 
 * <p>This class is not thread-safe.
 */
public final class GlobalLockModel {
  public static final class UnsupportedLockException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private final ExpressionNode bad;
    
    public UnsupportedLockException(final ExpressionNode en) { bad = en; }
    
    public ExpressionNode getUnsupportedLock() { return bad; }
  }
  
  
  
  /** Special IRNode used to represent 'this' */
  public static final IRNode THIS = new PlainIRNode(); 
  
  
  
  /** The binder to use. */
  private final IBinder binder;
  
  /**
   * Special class record that is the root of the hierarchy. We pretend that
   * this is the class in which {@link #THIS}is declared. Even class Object
   * descends from this bogus class.
   */
  private final ClassRecord rootRecord; 
  
  /**
   * Map from class declarations to class records.  The contents of 
   * this map are built by {@link #addRegionLockDeclaration(IBinder, IRNode, IRNode)}
   * and {@link #addPolicyLockDeclaration(IBinder, LockModel, IRNode)}.
   */
  private final Map<IJavaType, ClassRecord> classes =
    new HashMap<IJavaType, ClassRecord>();
  
  public GlobalLockModel(final IBinder bind) {
    super();
    this.binder = bind;
    this.rootRecord = new ClassRecord(null, null);
  }
 
  private synchronized ClassRecord getClassRecord(final IJavaType jt) {
    /* 2010-11-29: Need to use type erasure because type parameters are mucking
     * up the hierarchy.  In particular a parameterized subtype is showing up
     * as extending from Object instead of from the proper super class.
     */
    final ITypeEnvironment typeEnvironment = binder.getTypeEnvironment();
    final IJavaType erased = typeEnvironment.computeErasure(jt);
    ClassRecord cr = classes.get(erased);
    if (cr == null) {      
      final IJavaType parent = jt.getSuperclass(typeEnvironment);
      if (parent instanceof IJavaDeclaredType) {
        cr = new ClassRecord(getClassRecord(parent),  erased);
      } else {
        cr = new ClassRecord(rootRecord, erased);
      }
      classes.put(erased, cr);
    }
    return cr;
  }
  
  
  
  /**
   * Get the lock record for a lock with a given name for the given class
   * declaration.
   * 
   * @param clazz
   *          The type representation of the class.
   * @param name
   *          The lock name.
   * @return The lock record or <code>null</code> if the lock is not found.
   */
  public RegionLockRecord getRegionLockByName(final IJavaType clazz, final String name) {
    return getClassRecord(clazz).getRegionLockByName(name);
  }
  
  /**
   * Get the lock record for the policy lock with the given name for the given
   * class.
   * 
   * @param clazz
   *          The type of the class to lookup the name in.
   * @param name
   *          The lock name.
   * @return The lock record or <code>null</code> if the lock is not found.
   */
  public PolicyLockRecord getPolicyLockByName(final IJavaType clazz, final String name) {
    return getClassRecord(clazz).getPolicyLockByName(name);
  }

  /**
   * Get the lock records for the locks that protect the state of the given
   * class.
   * 
   * @param clazz
   *          The type representation of the class.
   * @return The Set of LockRecords.
   */
  public Set<RegionLockRecord> getRegionLocksInClass(final IJavaType clazz) {
    return getClassRecord(clazz).getRegionLocksForMyState();
  }

  /**
   * Get the policy lock records for the locks that protect the policy of the
   * given class.
   * 
   * @param clazz
   *          The type representation of the class
   * @return The Set of PolicyLockRecords.
   */
  public Set<PolicyLockRecord> getPolicyLocksInClass(final IJavaType clazz) {
    return getClassRecord(clazz).getPolicyLocksForMyState();
  }

  /**
   * Get the lock records for all the state and policy locks declared in the
   * class.
   * 
   * @param clazz
   *          The type representation of the class
   * @return The Set of AbstractLockRecord.
   */
  public Set<AbstractLockRecord> getRegionAndPolicyLocksInClass(final IJavaType clazz) {
    return getClassRecord(clazz).getStateAndPolicyLocksForMyState();
  }

  /**
   * Get the lock records for all the lock declarations that the given lock
   * implementation is used in.  If lockImpl is a static field then
   * the locks may come from classes that are not related to the current
   * class.  If lockImpl is THIS or an instance field, then the locks are
   * filtered: only locks declared by the current class or its ancestors
   * are returned.  This results in asymmetry in the treatment of static
   * and non-static fields, but it is justified by the differences between
   * static and instance fields in the class hierarchy, in general, and
   * by experience on actual code.
   * 
   * @param clazz
   *          The type representation of the class to base the look up on.
   * @param lockImpl
   *          An IRNode with operator type VariableDeclarator or
   *          ClassDeclaration, or the IRNode {@link GlobalLockModel#THIS}.
   * @return An unmodifiable set of LockRecords.
   */
  public Set<RegionLockRecord> getRegionLocksForLockImpl(final IJavaType clazz, final IRNode lockImpl) {
    return getClassRecord(clazz).getRegionLocksForLockImpl(binder, lockImpl);
  }

  /**
   * Get the policy lock records for all the policy lock declarations that the
   * given lock implementation is used in. If lockImpl is a static field then
   * the locks may come from classes that are not related to the current
   * class. If lockImpl is THIS or an instance field, then the locks are
   * filtered: only locks declared by the current class or its ancestors are
   * returned. This results in asymmetry in the treatment of static and
   * non-static fields, but it is justified by the differences between static
   * and instance fields in the class hierarchy, in general, and by experience
   * on actual code.
   * 
   * 
   * @param clazz
   *          The type representation of the class to base the look up on.
   * @param lockImpl
   *          An IRNode with operator type VariableDeclarator or
   *          ClassDeclaration, or the IRNode {@link GlobalLockModel#THIS}.
   * @return An unmodifiable set of PolicyLockRecords.
   */
  public Set<PolicyLockRecord> getPolicyLocksForLockImpl(final IJavaType clazz, final IRNode lockImpl) {
    return getClassRecord(clazz).getPolicyLocksForLockImpl(binder, lockImpl);
  }
  
  /**
   * Get the lock records for all the state and policy lock declarations that the
   * given lock implementation is used in. If lockImpl is a static field then
   * the locks may come from classes that are not related to the current
   * class. If lockImpl is THIS or an instance field, then the locks are
   * filtered: only locks declared by the current class or its ancestors are
   * returned. This results in asymmetry in the treatment of static and
   * non-static fields, but it is justified by the differences between static
   * and instance fields in the class hierarchy, in general, and by experience
   * on actual code.
   * 
   * 
   * @param clazz
   *          The type representation of the class to base the look up on.
   * @param lockImpl
   *          An IRNode with operator type VariableDeclarator or
   *          ClassDeclaration, or the IRNode {@link GlobalLockModel#THIS}.
   * @return An unmodifiable set of AbstractLockRecord.
   */
  public Set<AbstractLockRecord> getRegionAndPolicyLocksForLockImpl(
      final IJavaType clazz, final IRNode lockImpl) {
    return getClassRecord(clazz).getStateAndPolicyLocksForLockImpl(binder, lockImpl);
  }

  
  
  private static final class LockMap<T extends AbstractLockRecord> {
    /**
     * Tracks, for each field in the class, those locks that use the field 
     * as the lock implementation.  Can refer to locks that are declared
     * in classes that extend from the class this map is maintained for.
     */
    private final Map<IRNode, Set<T>> fieldsAsLocks = new HashMap<IRNode, Set<T>>();
    
    /**
     * Set of lock records that apply to the class.  These are the locks 
     * that are explicitly declared on the class.
     */
    private final Set<T> myLocks = new HashSet<T>();
    
    
    
    public LockMap() {
      super();
    }
    
    public void addLock(final T lockRec) {
      myLocks.add(lockRec);
    }
    
    public void addFieldUsedAsLock(final T lockRec) {
      Set<T> lockSet = fieldsAsLocks.get(lockRec.lockImpl);
      if (lockSet == null) {
        lockSet = new HashSet<T>();
        fieldsAsLocks.put(lockRec.lockImpl, lockSet);
      }
      lockSet.add(lockRec);
    }

    public Set<T> getFieldAsLocks(final IRNode lockImpl) {
      return fieldsAsLocks.get(lockImpl);
    }
    
    public Set<T> getLocks() {
      return myLocks;
    }
  }
  
  
  
  private final class ClassRecord {
    private final ClassRecord parent;
    private final IJavaType classDecl;
    
    /** Lock map for state locks */
    private final LockMap<RegionLockRecord> lockMap = new LockMap<RegionLockRecord>();
    
    /** Lock map for policy locks */
    private final LockMap<PolicyLockRecord> plockMap = new LockMap<PolicyLockRecord>();

    
    
    private ClassRecord(final ClassRecord p, final IJavaType cd) {
      parent = p;
      classDecl = cd;
    }

    
    
    private <T extends AbstractLockRecord> T getLockByName(
        final Set<T> locks, final String name) {    
      for (final T lr : locks) {
        if (lr.name.equals(name)) return lr;
      }
      return null;
    }
    
    /**
     * Get the lock record for the lock of the given name that protects
     * some of the state of this class.
     * @param name The lock name
     * @return The lock record or <code>null</code> if not found.
     */
    private RegionLockRecord getRegionLockByName(final String name) {
      return getLockByName(getRegionLocksForMyState(), name);
    }
    
    /**
     * Get the policy lock record for the lock of the given name that protects
     * policy of this class.
     * @param name The lock name
     * @return The policy lock record or <code>null</code> if not found.
     */
    private PolicyLockRecord getPolicyLockByName(final String name) {
      return getLockByName(getPolicyLocksForMyState(), name);
    }

    private <T1 extends AbstractLockRecord, T2 extends T1> void filterLocks(
        final IBinder binder, final IRNode lockImpl,
        final Set<T2> allLockRecords, final Set<T1> filteredLockRecords) {
      if (allLockRecords != null) {
        /* Only filter if lock is 'this' or an instance field.  A static field
         * is allowed to be used as a lock for unrelated classes.  It doesn't
         * make sense for an instance field to be used as a lock for an
         * unrelated class because there isn't any way to indicate which
         * *instance* is the holder of the field being used as a lock.  
         * 
         * Not filtering static fields does lead to some asymmetry in the
         * lock semantics.  Assume field f of class C is used as a lock
         * by class D extends C.  If f is static then reference C.f will
         * resolve to being a lock for class D.  If f is not static then
         * reference c.f, where c is statically a reference to an object of type
         * C, will NOT resolve to a lock for class D.  This just makes more
         * sense heuristically, based on what we want the behavior to be for
         * real code.
         */
        boolean filter = lockImpl.equals(THIS);
        if (!filter) {
          if (VariableDeclarator.prototype.includes(JJNode.tree.getOperator(lockImpl))) {
            filter = !TypeUtil.isStatic(lockImpl);
          }
        }
        if (filter) {
          /* Only want locks declared in me or my ancestors, i.e., from
           * classes that I am a subtype of.
           */
          final ITypeEnvironment typeEnv = binder.getTypeEnvironment();
          for (final T2 lr : allLockRecords) {
            if (typeEnv.isRawSubType(this.classDecl, lr.classDecl)) {
              filteredLockRecords.add(lr);
            }
          }
        } else {
          filteredLockRecords.addAll(allLockRecords);
        }
      }
    }

    /**
     * Get the state lock records for all the lock declarations that the given lock
     * implementation is used in.  If lockImpl is a static field then
     * the locks may come from classes that are not related to the current
     * class.  If lockImpl is THIS or an instance field, then the locks are
     * filtered: only locks declared by the current class or its ancestors
     * are returned.  This results in asymmetry in the treatment of static
     * and non-static fields, but it is justified by the differences between
     * static and instance fields in the class hierarchy, in general, and
     * by experience on actual code.
     * 
     * @param lockImpl
     *          An IRNode with operator type VariableDeclarator or
     *          ClassDeclaration, or the IRNode {@link GlobalLockModel#THIS}.
     * @return An unmodifiable set of LockRecords.
     */
    private Set<RegionLockRecord> getRegionLocksForLockImpl(
        final IBinder binder, final IRNode lockImpl) {
      final ClassRecord definingClass = getClassDefiningLock(lockImpl);
      final Set<RegionLockRecord> allLockRecords = definingClass.lockMap.getFieldAsLocks(lockImpl);
      final Set<RegionLockRecord> filteredRecords = new HashSet<RegionLockRecord>();
      filterLocks(binder, lockImpl, allLockRecords, filteredRecords);
      return Collections.unmodifiableSet(filteredRecords);
    }

    /**
     * Get the policy lock records for all the policy lock declarations that the
     * given lock implementation is used in. If lockImpl is a static field then
     * the locks may come from classes that are not related to the current
     * class. If lockImpl is THIS or an instance field, then the locks are
     * filtered: only locks declared by the current class or its ancestors are
     * returned. This results in asymmetry in the treatment of static and
     * non-static fields, but it is justified by the differences between static
     * and instance fields in the class hierarchy, in general, and by experience
     * on actual code.
     * 
     * @param lockImpl
     *          An IRNode with operator type VariableDeclarator or
     *          ClassDeclaration, or the IRNode {@link GlobalLockModel#THIS}.
     * @return An unmodifiable set of LockRecords.
     */
    private Set<PolicyLockRecord> getPolicyLocksForLockImpl(
        final IBinder binder, final IRNode lockImpl) {
      final ClassRecord definingClass = getClassDefiningLock(lockImpl);
      final Set<PolicyLockRecord> allLockRecords = definingClass.plockMap.getFieldAsLocks(lockImpl);
      final Set<PolicyLockRecord> filteredRecords = new HashSet<PolicyLockRecord>();
      filterLocks(binder, lockImpl, allLockRecords, filteredRecords);
      return Collections.unmodifiableSet(filteredRecords);
    }

    /**
     * Get the lock records for all the state and policy lock declarations that the
     * given lock implementation is used in. If lockImpl is a static field then
     * the locks may come from classes that are not related to the current
     * class. If lockImpl is THIS or an instance field, then the locks are
     * filtered: only locks declared by the current class or its ancestors are
     * returned. This results in asymmetry in the treatment of static and
     * non-static fields, but it is justified by the differences between static
     * and instance fields in the class hierarchy, in general, and by experience
     * on actual code.
     * 
     * @param lockImpl
     *          An IRNode with operator type VariableDeclarator or
     *          ClassDeclaration, or the IRNode {@link GlobalLockModel#THIS}.
     * @return An unmodifiable set of LockRecords.
     */
    private Set<AbstractLockRecord> getStateAndPolicyLocksForLockImpl(
        final IBinder binder, final IRNode lockImpl) {
      final ClassRecord definingClass = getClassDefiningLock(lockImpl);
      final Set<RegionLockRecord> allStateLockRecords = definingClass.lockMap.getFieldAsLocks(lockImpl);
      final Set<PolicyLockRecord> allPolicyLockRecords = definingClass.plockMap.getFieldAsLocks(lockImpl);
      final Set<AbstractLockRecord> filteredRecords = new HashSet<AbstractLockRecord>();
      filterLocks(binder, lockImpl, allStateLockRecords, filteredRecords);
      filterLocks(binder, lockImpl, allPolicyLockRecords, filteredRecords);
      return Collections.unmodifiableSet(filteredRecords);
    }    
    
    /**
     * Get the lock records for all the lock declarations that apply to 
     * this class.
     * @return An unmodifiable set of lock records
     */
    private Set<RegionLockRecord> getRegionLocksForMyState() {
      final Set<RegionLockRecord> lockRecords = new HashSet<RegionLockRecord>();
      ClassRecord current = this;
      while (current != null) {
        lockRecords.addAll(current.lockMap.getLocks());
        current = current.parent;
      }
      return Collections.unmodifiableSet(lockRecords);
    }    
    
    /**
     * Get the policy lock records for all the lock declarations that apply to 
     * this class.
     * @return An unmodifiable set of policy lock records
     */
    private Set<PolicyLockRecord> getPolicyLocksForMyState() {
      final Set<PolicyLockRecord> lockRecords = new HashSet<PolicyLockRecord>();
      ClassRecord current = this;
      while (current != null) {
        lockRecords.addAll(current.plockMap.getLocks());
        current = current.parent;
      }
      return Collections.unmodifiableSet(lockRecords);
    }
    
    /**
     * Get the lock records for all the state and policy lock declarations that
     * apply to this class.
     * 
     * @return An unmodifiable set of policy lock records
     */
    private Set<AbstractLockRecord> getStateAndPolicyLocksForMyState() {
      final Set<AbstractLockRecord> lockRecords = new HashSet<AbstractLockRecord>();
      ClassRecord current = this;
      while (current != null) {
        lockRecords.addAll(current.lockMap.getLocks());
        lockRecords.addAll(current.plockMap.getLocks());
        current = current.parent;
      }
      return Collections.unmodifiableSet(lockRecords);
    }
  }
  
  /**
   * Given an expression that evaluates to an object-typed expression,
   * return the locks associated with the receiver of the expression's type.
   * That is, given <code>o.f</code>, where the expression <code>o.f</code>
   * has type <code>C</code>, return the locks in <code>C</code> whose
   * representation is <code>this</code>.
   * 
   * @param expr The IRNode of the expression.
   * @return The set of lock records as described above as an unmodifiable set.
   */
  public Set<RegionLockRecord> getRegionLocksForSelf(final IRNode expr) {
    final IJavaType type = binder.getJavaType(expr);
    final ClassRecord cr = getClassRecord(type);
    return cr.getRegionLocksForLockImpl(binder, THIS);
  }
 
  /**
   * Given an expression that evaluates to an object-typed expression, return
   * the policy locks associated with the receiver of the expression's type.
   * That is, given <code>o.f</code>, where the expression <code>o.f</code>
   * has type <code>C</code>, return the policy locks in <code>C</code>
   * whose representation is <code>this</code>.
   * 
   * @param expr
   *          The IRNode of the expression.
   * @return The set of policy lock records as described above as an unmodifiable set.
   */
  public Set<PolicyLockRecord> getPolicyLocksForSelf(final IRNode expr) {
    final IJavaType type = binder.getJavaType(expr);
    final ClassRecord cr = getClassRecord(type);
    return cr.getPolicyLocksForLockImpl(binder, THIS);
  }
  
  /**
   * Given an expression that evaluates to an object-typed expression, return
   * the state and policy locks associated with the receiver of the expression's type.
   * That is, given <code>o.f</code>, where the expression <code>o.f</code>
   * has type <code>C</code>, return the state and policy locks in <code>C</code>
   * whose representation is <code>this</code>.
   * 
   * @param expr
   *          The IRNode of the expression.
   * @return The set of lock records as described above as an unmodifiable set.
   */
  public Set<AbstractLockRecord> getRegionAndPolicyLocksForSelf(final IRNode expr) {
    final IJavaType type = binder.getJavaType(expr);
    final Set<AbstractLockRecord> result = new HashSet<AbstractLockRecord>();
    final ClassRecord cr = getClassRecord(type);
    result.addAll(cr.getRegionLocksForLockImpl(binder, THIS));
    result.addAll(cr.getPolicyLocksForLockImpl(binder, THIS));
    return Collections.unmodifiableSet(result);
  }
  
  private ClassRecord getClassDefiningLock(final IRNode lockImpl) {
    final ClassRecord classDefiningLock;
    if (lockImpl.equals(THIS)) {
      // The class whose field is a lock is the root class class
      classDefiningLock = rootRecord;
    } else { 
      if (TypeDeclaration.prototype.includes(JJNode.tree.getOperator(lockImpl))) {
        // The class whose field is a lock is the named class
        classDefiningLock = getClassRecord(binder.getTypeEnvironment().getMyThisType(lockImpl));
      } else {
        // The class whose field is the lock is the class that contains the field declaration
        classDefiningLock = 
        	getClassRecord(binder.getTypeEnvironment().getMyThisType(VisitUtil.getEnclosingType(lockImpl)));
      }
    }
    return classDefiningLock;
  }
  
  /**
   * Update the global lock information with a new lock annotation
   * 
   * @param binder
   *          The binder.
   * @param lockDecl
   *          The lock annotation to add.
   * @param classDecl
   *          The type representation of the class on which the lock annotation appears.
   */
  public void addRegionLockDeclaration(
      final IBinder binder, final LockModel lockDecl, final IJavaDeclaredType clazz) {
    try {
      final RegionLockRecord lockRec = new RegionLockRecord(binder, clazz, lockDecl);
      final ClassRecord annotatedClass = getClassRecord(clazz);
      annotatedClass.lockMap.addLock(lockRec);
       
      final ClassRecord classDefiningLock = getClassDefiningLock(lockRec.lockImpl);
      classDefiningLock.lockMap.addFieldUsedAsLock(lockRec);
    } catch (final GlobalLockModel.UnsupportedLockException e) {
      // Do nothing.  Unsupported locks are not kept in the model
    }
  }
  
  /**
   * Update the global lock information with a new policy lock annotation
   * 
   * @param binder
   *          The binder.
   * @param lockDecl
   *          The policy lock annotation to add.
   * @param clazz
   *          The type representation of the class on which the policy lock annotation appears.
   */
  public void addPolicyLockDeclaration(
      final IBinder binder, final LockModel lockDecl, final IJavaDeclaredType clazz) {
    try {
      final PolicyLockRecord lockRec = new PolicyLockRecord(binder, clazz, lockDecl);
      final ClassRecord annotatedClass = getClassRecord(clazz);
      annotatedClass.plockMap.addLock(lockRec);
       
      final ClassRecord classDefiningLock = getClassDefiningLock(lockRec.lockImpl);
      classDefiningLock.plockMap.addFieldUsedAsLock(lockRec);
    } catch (final GlobalLockModel.UnsupportedLockException e) {
      // Do nothing.  Unsupported locks are not kept in the model
    }
  }

  /**
   * Given a lock implementation taken from the lock declaration, get the
   * globally unique IRNode that represents it. This method is public primarily
   * because it is used by the LockRecord and PolicyLockRecord classes, but it
   * is more also public because it is a generally useful service provided by
   * this class.
   * 
   * @param binder
   *          The binder to use.
   * @param lockImpl
   *          The lock implementation as returned from
   *          {@link edu.cmu.cs.fluid.java.promise.LockDeclaration#getField(IRNode)}.
   * @return {@link GlobalLockModel#THIS}if the given lock implementation is a
   *         ThisExpression; otherwise, the result of binding the lock
   *         implementation is returned, which yields a ClassDeclaration node in
   *         the case of ClassExpressions, or a VariableDeclarator in the case
   *         of a named field.
   */
  public static IRNode canonicalizeLockImpl(final IBinder binder,
      final ExpressionNode lockImpl) throws UnsupportedLockException {
    if (lockImpl instanceof ThisExpressionNode) {
      return THIS;
    } else if (lockImpl instanceof ClassLockExpressionNode) {
      final ClassLockExpressionNode cls = (ClassLockExpressionNode) lockImpl;
      return cls.resolveType().getNode();
    } else if (lockImpl instanceof QualifiedThisExpressionNode) {
      throw new UnsupportedLockException(lockImpl);
    } else {
      final FieldRefNode ref = (FieldRefNode) lockImpl;
      return ref.resolveBinding().getNode();
    }
  }
}
