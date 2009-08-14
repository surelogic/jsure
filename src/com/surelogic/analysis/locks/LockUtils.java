/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/LockUtils.java,v 1.66 2009/02/17 14:01:32 aarong Exp $*/
package com.surelogic.analysis.locks;

import com.surelogic.aast.java.*;
import com.surelogic.aast.promise.*;
import com.surelogic.analysis.MethodCallUtils;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.effects.*;
import com.surelogic.analysis.effects.targets.ClassTarget;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetFactory;
import com.surelogic.analysis.effects.targets.ThisBindingTargetFactory;
import com.surelogic.analysis.locks.locks.HeldLock;
import com.surelogic.analysis.locks.locks.HeldLockFactory;
import com.surelogic.analysis.locks.locks.ILock;
import com.surelogic.analysis.locks.locks.NeededLock;
import com.surelogic.analysis.locks.locks.NeededLockFactory;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.*;
import com.surelogic.util.NullSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IOldTypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.java.operator.ArrayRefExpression;
import edu.cmu.cs.fluid.java.operator.CharLiteral;
import edu.cmu.cs.fluid.java.operator.ClassExpression;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.IntLiteral;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Hashtable2;


/**
 * Class to hold utility methods used by the lock analyses.  Many of these
 * methods used to be in {@link LockVisitor} or {@link InstanceLock}.  This 
 * class exists to prevent mutual coupling between LockVisitor and the
 * data-flow analyses needed to support java.util.concurrent.Locks.
 * 
 * <p>This class is parameterized by the Java name binder, as well as various
 * sub analyses that are needed to support lock analyses.  During each analysis
 * run, an instance of this class is shared by the LockVisitor and flow analyses
 * used to perform lock analysis.  
 * 
 * @author aarong
 */
public final class LockUtils {
  /** The name of the interface {@code java.util.concurrent.locks.Lock}. */
  public static final String JAVA_UTIL_CONCURRENT_LOCKS_LOCK = "java.util.concurrent.locks.Lock";

  /** The name of the interface {@code java.util.concurrent.locks.ReadWriteLock}. */
  public static final String JAVA_UTIL_CONCURRENT_LOCKS_READWRITELOCK = "java.util.concurrent.locks.ReadWriteLock";

  /**
   * The name of the {@code unlock} method of
   * {@code java.util.concurrent.locks.Lock}.
   */
  public static final String UNLOCK = "unlock";

  /**
   * The name of the {@code lock} method of
   * {@code java.util.concurrent.locks.Lock}.
   */
  public static final String LOCK = "lock";

  /**
   * The name of the {@code lockInterruptibly} method of
   * {@code java.util.concurrent.locks.Lock}.
   */
  public static final String LOCKINTERRUPTIBLY = "lockInterruptibly"; 
  
  /**
   * The name of other {@code readLock} method of
   * {code java.util.concurrent.locks.ReadWriteLock}.
   */
  public static final String READLOCK = "readLock";

  /**
   * The name of other {@code writeLock} method of
   * {code java.util.concurrent.locks.ReadWriteLock}.
   */
  public static final String WRITELOCK = "writeLock";

  

  /**
   * The model of all the locks in the system.
   * @see LockVisitor#sysLockModelHandle
   */
  private AtomicReference<GlobalLockModel> sysLockModelHandle;

  /** The Java name binder to use. */
  private final IBinder binder;
  
  /** The effects analysis to use. */
  private final EffectsVisitor effectsVisitor;
  
  private final ConflictChecker conflicter;
  
  /** Factory for creating held locks */
  private final HeldLockFactory heldLockFactory;
  
  /** Factory for creating needed locks */
  private final NeededLockFactory neededLockFactory;
  
  /** Factory for creating targets */
  private final TargetFactory targetFactory;
  
  
  
  /** The element region: {@code []}. */
  private final RegionModel elementRegion;
  
  /**
   * The internal representation of the {@link java.util.concurrent.locks.Lock}
   * interface.  We look this up once in the type environment during 
   * construction to avoid repeated lookups.
   */
  private final IJavaDeclaredType lockType;
  
  /**
   * The internal representation of the {@link java.util.concurrent.locks.ReadWriteLock}
   * interface.  We look this up once in the type environment during 
   * construction to avoid repeated lookups.
   */
  private final IJavaDeclaredType readWriteLockType;

  /**
   * Cache used to speed up isMethodFrom()
   */
  private final Hashtable2<IJavaType, IJavaType, Boolean> subTypeCache = 
	  new Hashtable2<IJavaType, IJavaType, Boolean>();
  
  // ========================================================================

  // Poor man's tuple
  public static class GoodAndBadLocks<T extends ILock> {
    public Set<T> goodLocks = new HashSet<T>();
    public Set<LockSpecificationNode> badLocks = new HashSet<LockSpecificationNode>();
    
    public GoodAndBadLocks() {
      // do nothing;
    }
  }
  
  // ========================================================================
  
  
  
  
  /**
   * Create a set of lock utility methods based around a given set of 
   * analysis information.
   * @param glm The global lock model to use.
   * @param b The Java name binder to use.
   * @param ea The effects analysis to use.
   */
  public LockUtils(final AtomicReference<GlobalLockModel> glmRef,
      final IBinder b, final EffectsVisitor ev, final IAliasAnalysis aliasAnalysis,
      final HeldLockFactory hlf, final NeededLockFactory nlf,
      final ThisExpressionBinder thisExprBinder) {
    sysLockModelHandle = glmRef;
    binder = b;
    effectsVisitor = ev;
    conflicter = new ConflictChecker(b, aliasAnalysis);
    heldLockFactory = hlf;
    neededLockFactory = nlf;
    targetFactory = new ThisBindingTargetFactory(thisExprBinder);
    
    lockType = (IJavaDeclaredType) JavaTypeFactory.convertNodeTypeToIJavaType(
          binder.getTypeEnvironment().findNamedType(JAVA_UTIL_CONCURRENT_LOCKS_LOCK),
          binder);
    readWriteLockType = (IJavaDeclaredType) JavaTypeFactory.convertNodeTypeToIJavaType(
        binder.getTypeEnvironment().findNamedType(JAVA_UTIL_CONCURRENT_LOCKS_READWRITELOCK),
        binder);

    // Get the region for array elements
    elementRegion = RegionModel.getInstance(PromiseConstants.REGION_ELEMENT_NAME);
    elementRegion.setNode(IOldTypeEnvironment.arrayType);
  }

  synchronized void clear() {
	subTypeCache.clear();
  }

  // ========================================================================
  // == Test for final expressions
  // ========================================================================

  /**
   * See if a given expression is a "final expression," that is an expression
   * whose value is fixed no matter where in the synchronized block it is 
   * evaluated.
   * 
   * @param expr
   *          The expression to test
   * @param sync
   *          The synchronized block whose lock expression contains <code>expr</code>.
   *          May be <code>null</code> if the expression is not part of the
   *          lock expression of a synchronized block.
   */
  /*
   * TODO Need to modify this method to produce
   * a nested chain of evidence of why it believes an expression to NOT be a
   * final expression. Chain should contain links to the field/var declaration
   * showing that the variable is not declared to be final, etc.
   */
  public boolean isFinalExpression(final IRNode expr, final IRNode sync) {
    final Operator op = JJNode.tree.getOperator(expr);
    if (MethodCall.prototype.includes(op)) {
      MethodCall mcall = (MethodCall) op;
      /* Object expression must be final or method must be static, and the method
       * must have a @returnsLock annotation (which we are using as a very
       * specific idempotency annotation) or a call to readLock() or writeLock().
       */
      final IRNode mdecl = binder.getBinding(expr);
      if (TypeUtil.isStatic(mdecl)
          || isFinalExpression(mcall.get_Object(expr), sync)) {
        return (getReturnedLock(mdecl) != null) || isReadWriteLockClassUsage(expr);
      }
    } else if (ClassExpression.prototype.includes(op)) {
      /* Class expressions are special field refs */
      return true;
    } else if (ThisExpression.prototype.includes(op)) {
      /*Use of the receiver is a final expression */
      return true;
    } else if (QualifiedThisExpression.prototype.includes(op)) {
      /* Use of the receiver of the outer object is a final expression */
      return true;
    } else if (VariableUseExpression.prototype.includes(op)) {
      /* Local variable/parameter must be declared to be final, or be unmodified
       * within the synchronized block
       */
      final IRNode id = binder.getBinding(expr);
      if (TypeUtil.isFinal(id)) {
        return true;
      } else {
        return !isLockExpressionChangedBySyncBlock(expr, sync);
      }
    } else if (FieldRef.prototype.includes(op)) {
      /* Field must be final (or protected by a lock and not modified by the
       * body of the synchronized block) AND either the field must be static or
       * the object expression must be final
       */
      final IRNode id = binder.getBinding(expr);

      /* Check that the object expression is final (or static) */
      if (TypeUtil.isStatic(id)
          || isFinalExpression(FieldRef.getObject(expr), sync)) {
        // Check if the field is final
        if (TypeUtil.isFinal(id)) {
          return true;
        } else {
          /* Check if the field is protected by a lock and is not modified by
           * the body of the synchronized block.
           */
          // Doesn't matter if we are reading/write here, just want a lock or not
          return !getLockForFieldRef(expr, false).isEmpty()
              && !isLockExpressionChangedBySyncBlock(expr, sync);
        }
      }
    } else if (ArrayRefExpression.prototype.includes(op)) {
      /* Array ref expression e[e']. Expressions e and e' must be final
       * expressions. The effects of the synchronized block must not conflict
       * with reading from the array.
       */
      final IRNode array = ArrayRefExpression.getArray(expr);
      final IRNode idx = ArrayRefExpression.getIndex(expr);
      if (isFinalExpression(array, sync) && isFinalExpression(idx, sync)) {
        return !isArrayChangedBySyncBlock(array, sync, expr);
      }
    } else if (IntLiteral.prototype.includes(op)) {
      /* Integer constants are final.  We do not consider float, boolean, or
       * String literals because they cannot be used with array expressions.
       * What we are targeting here is the case "array[5]" or "array['g']"
       * (handled by CharLiteral below).
       */
      return true;
    } else if (CharLiteral.prototype.includes(op)) {
      // See IntLiteral (above)
      return true;
    }
    return false;
  }

  /**
   * Determines whether the effects of the given synchronized block
   * conflict with the given lock expression.  Really what we want to know is
   * whether the synchronized block writes to anything that the could affect
   * the value of the lock expression.
   * 
   * @param expr A lock expression
   * @param sync A synchronized statement.
   * @return Whether the effects conflict; if {@code sync} is {@code null} then
   * this always returns {@code true} because we do not have a well-defined
   * syntactic scope inside of which to limit the usage of the lock expression.
   */
  private boolean isLockExpressionChangedBySyncBlock(final IRNode expr, final IRNode sync) {
    if (sync != null) {
      final Set<Effect> bodyEffects = effectsVisitor.getEffects(sync);
      final Set<Effect> exprEffects = effectsVisitor.getEffects(expr);
      return conflicter.mayConflict(bodyEffects, exprEffects, expr);
    } else {
      
      return true;
    }
  }

  private boolean isArrayChangedBySyncBlock(final IRNode array, final IRNode sync,
      final IRNode compareBeforeNode) {
    if (sync != null) {
      final Set<Effect> exprEffects =
        Collections.singleton(Effect.newRead(targetFactory.createInstanceTarget(array, elementRegion)));
      final Set<Effect> bodyEffects = effectsVisitor.getEffects(sync);
      return conflicter.mayConflict(bodyEffects, exprEffects, compareBeforeNode);
    } else {
      return true;
    }
  }
  
  
  
  // ========================================================================
  // == Test for use of java.util.concurrent
  // ========================================================================

  public boolean isJUCRWMethod(final IRNode mcall) {
    final ReadWriteLockMethods rwLockMethod = 
      this.whichReadWriteLockMethod(mcall);
    return rwLockMethod == ReadWriteLockMethods.READLOCK
        || rwLockMethod == ReadWriteLockMethods.WRITELOCK;
  }
  
  private synchronized boolean isSubType(IJavaType s, IJavaType t) {
	  if (s == null) {
		  return false;
	  }
	  Boolean result = subTypeCache.get(s, t);
	  if (result != null) {
		  return result.booleanValue();
	  }
	  boolean rv = binder.getTypeEnvironment().isSubType(s, t);
	  subTypeCache.put(s, t, rv);
	  return rv;
  }
  
  private boolean isMethodFrom(final IRNode mcall, final IJavaDeclaredType testType) { //final String testClassName) {
	  IBinding b = binder.getIBinding(mcall);
	  IJavaDeclaredType context = b.getContextType();
	  if (context == null) {
		  IRNode tdecl = VisitUtil.getEnclosingType(b.getNode());
		  context = (IJavaDeclaredType) binder.getTypeEnvironment().convertNodeTypeToIJavaType(tdecl);
	  }
	  return isSubType(context, testType);
  }
  
  /**
   * Test if the given method call node calls a method declared in 
   * interface {@code java.util.concurrent.locks.Lock}.
   */
  private boolean isMethodFromJavaUtilConcurrentLocksLock(final IRNode mcall) {
    return isMethodFrom(mcall, lockType);
  }
  
  /**
   * Test if the given method call node calls a method declared in 
   * interface {@code java.util.concurrent.locks.Lock}.
   */
  private boolean isMethodFromJavaUtilConcurrentLocksReadWriteLock(final IRNode mcall) {
    return isMethodFrom(mcall, readWriteLockType);
  }
  
  /**
   * Return which method from {@code java.util.concurrent.locks.Lock} is
   * being called.
   */
  public LockMethods whichLockMethod(final IRNode mcall) {
    final LockMethods method = LockMethods.whichLockMethod(MethodCall.getMethod(mcall));
    if (isMethodFromJavaUtilConcurrentLocksLock(mcall)) {
      return method;
    } else {
      return method == LockMethods.NOT_A_LOCK_METHOD ? method : LockMethods.IDENTICALLY_NAMED_METHOD;
    }
  }
  
  /**
   * Return which method from {@code java.util.concurrent.locks.Lock} is
   * being called.
   */
  public ReadWriteLockMethods whichReadWriteLockMethod(final IRNode mcall) {
    final ReadWriteLockMethods method = ReadWriteLockMethods.whichReadWriteLockMethod(MethodCall.getMethod(mcall));
    if (isMethodFromJavaUtilConcurrentLocksReadWriteLock(mcall)) {
      return method;
    } else {
      return method == ReadWriteLockMethods.NOT_A_READWRITELOCK_METHOD ? method : ReadWriteLockMethods.IDENTICALLY_NAMED_METHOD;
    }
  }
  
  /**
   * Test if the method call node calls any method from the
   * {@code java.util.concurrent.locks.Lock} class.
   * 
   * @param mcall
   *          A MethodCall node.
   */
  public boolean isLockClassUsage(final IRNode mcall) {
    return isMethodFromJavaUtilConcurrentLocksLock(mcall);
  }
  
  /**
   * Test if the method call node calls any method from the
   * {@code java.util.concurrent.locks.ReadWriteLock} class.
   * 
   * @param mcall
   *          A MethodCall node.
   */
  public boolean isReadWriteLockClassUsage(final IRNode mcall) {
    return isMethodFromJavaUtilConcurrentLocksReadWriteLock(mcall);
  }  

  /**
   * Test if a class implements {@code java.util.concurrent.locks.Lock}.
   * 
   * @param type
   *          The java type to test
   */
  public boolean implementsLock(final IJavaType type) {
    if (type instanceof IJavaDeclaredType) {
      return binder.getTypeEnvironment().isSubType(type, lockType);
    } else {
      // Arrays and primitives are not lock types
      return false;
    }
  }

  /**
   * Test if a class implements {@code java.util.concurrent.locks.ReadWriteLock}.
   * 
   * @param type
   *          The java type to test
   */
  public boolean implementsReadWriteLock(final IJavaType type) {
    if (type instanceof IJavaDeclaredType) {
      return binder.getTypeEnvironment().isSubType(type, readWriteLockType);
    } else {
      // Arrays and primitives are not lock types
      return false;
    }
  }

  
  
  // ========================================================================
  // == Annotation Getter Methods
  // ========================================================================

  /**
   * Get the lock returned by a method.
   * 
   * @param mdecl
   *          A MethodDeclaration node
   * @return a LockName node or <code>null</code> if the method doesn't return
   *         a lock.
   */
  public static ReturnsLockPromiseDrop getReturnedLock(final IRNode mdecl) {
    final IRNode returnNode = JavaPromise.getReturnNodeOrNull(mdecl);
    return (returnNode == null) ? null :
      LockRules.getReturnsLock(returnNode);
  }

  
  
  // ========================================================================
  // == Methods for getting the necessary locks
  // ========================================================================

  /**
   * Is the region reference supposed to be protected by a lock, and if so,
   * which one?
   * 
   * @param ref
   *          A FieldRef or ArrayRefExpression
   * @return Set of Locks that must be held or <code>null</code> if no lock is
   *         known to be necessary for the accessed region.
   */
  public Set<NeededLock> getLockForRegionRef(
      final IRNode ref, final boolean isWrite) {
    if (FieldRef.prototype.includes(JJNode.tree.getOperator(ref))) {
      return getLockForFieldRef(ref, isWrite);
    } else {
      return getLockForArrayRef(ref, isWrite);
    }
  }
  
  public Set<NeededLock> getLockForFieldRef(
      final IRNode fieldRef, final boolean isWrite) {
    return getLockForFieldRef(
        FieldRef.getObject(fieldRef), binder.getBinding(fieldRef), null, isWrite);    
  }

  /**
   * Is the array reference supposed to be protected, and if so, by which lock?
   * 
   * @param arrayRef
   *          An ArrayRefExpression node
   * @return The Lock that must be held or <code>null</code> if no lock is
   *         known to be necessary for the accessed region.
   */
  public Set<NeededLock> getLockForArrayRef(
      final IRNode arrayRef, final boolean isWrite) {
    final IRNode obj = ArrayRefExpression.getArray(arrayRef);
    return getLockForInstanceRegion(isWrite, obj, elementRegion);
  }
  
  public Set<NeededLock> getLocksForDirectRegionAccess(
    final IRNode srcNode, final boolean isRead, final Target target) {
    final Set<Effect> elaboratedEffects =
      effectsVisitor.elaborateEffect(targetFactory, srcNode, isRead, target);
    
    final Set<NeededLock> neededLocks = new HashSet<NeededLock>();
    for (final Effect effect : elaboratedEffects) {
      getLocksFromEffect(effect, neededLocks);
    }
    return Collections.unmodifiableSet(neededLocks);
  }
  
  /**
   * Is this field reference supposed to be protected, and if so, by which lock?
   * The parameters to this method are a little strange so that the same 
   * implementation can be used by both {@link #getLockForFieldRef(IRNode, boolean)}
   * and {@link #getLockForVarDecl(IRNode, IRNode, IJavaType)}.
   * 
   * @param obj
   *          The IRNode of the object expression portion of the field
   *          reference. Generally this is obtained using
   *          <code>FieldRef.getObject()</code>.
   * @param varDecl
   *          The field declaration node of the field being accessed. Generally
   *          this is obtained by binding the FieldRef node.
   * @param clazz
   *          The IJavaType of the class the field belongs to, or
   *          <code>null</code> if the method should figure this out using
   *          <code>binder.getJavaType(obj)</code>. This parameter exists so
   *          that the calling context can give the type if is already
   *          available. If not, it is best to pass <code>null</code> because
   *          the IJavaType is only interesting if the field is static.
   * @param isWrite
   *          <code>true</code> if the write permission is required.
   * @return The locks that must be held or the empty set if no lock is known to
   *         be necessary for the accessed region.
   */
  private Set<NeededLock> getLockForFieldRef(
      final IRNode obj, final IRNode varDecl, final IJavaType clazz,
      final boolean isWrite) {
    // final fields and volatile fields don't need to be protected
    if (!TypeUtil.isFinal(varDecl) && !TypeUtil.isVolatile(varDecl)) {
      final IRegion fieldAsRegion = RegionModel.getInstance(varDecl);

      /*
       * NOTE: Static regions cannot be aggregated into other regions, so we
       * don't have to do anything fancy here. Instance regions can be
       * aggregated into other regions, so we pass the buck to the
       * getLockForInstanceRegion() method which chases the aggregation chain to
       * see if a field access needs to be protected because the object it
       * belongs to has been aggregated into a protected region.
       */
      if (fieldAsRegion.isStatic()) {
        final IJavaType clazz2 = (clazz == null) ? binder.getJavaType(obj) : clazz;
        final RegionLockRecord neededLock = (clazz2 instanceof IJavaDeclaredType)
            ? getLockForRegion((IJavaDeclaredType) clazz2, fieldAsRegion)
            : null;

        // If we found a suitable region, generate the Lock
        if (neededLock != null) {
          /* Whether field is being written to determines whether we need a read
           * or write lock.
           */ 
          return Collections.<NeededLock> singleton(
              neededLockFactory.createStaticLock(neededLock.lockDecl, isWrite));
        } else {
          return Collections.emptySet();
        }
      } else {
        /* Whether field is being written to determines whether we need a read
         * or write lock.
         */ 
        return getLockForInstanceRegion(isWrite, obj, fieldAsRegion);
      }
    }
    return Collections.emptySet();
  }

  /**
   * Get the lock record for the lock that protects the given region in the
   * given class.
   * 
   * @param clazz
   *          Type representation of the class 
   * @param fieldAsRegion
   *          A region of the given class.
   * @return The lock declaration for the lock that protects the given region,
   *         which may in fact be associated with a super region.
   *         <code>null</code> if the region is unprotected.
   */
  public RegionLockRecord getLockForRegion(
      final IJavaDeclaredType clazz, final IRegion fieldAsRegion) {
    final Set<RegionLockRecord> stateLocksInClass = sysLockModelHandle.get().getRegionLocksInClass(clazz);
    for (final RegionLockRecord lr : stateLocksInClass) {
      if (lr.region.ancestorOf(fieldAsRegion)) {
        return lr;
      }
    }
    return null;
  }
  
  /**
   * Get the lock needed for the field referenced in the given FieldRef
   * expression.
   */
  public RegionLockRecord getLockForFieldRef(final IRNode fieldRef) {
    return getLockForRegion(
        (IJavaDeclaredType) binder.getJavaType(FieldRef.getObject(fieldRef)),
        RegionModel.getInstance(binder.getBinding(fieldRef)));
  }

  /**
   * Given a reference to an instance region, find the locks that protect that
   * region, using aggregation information.
   * 
   * @param src
   *          The reference to the region, either a FieldRef,
   *          ArrayRefExpression, or VariableDeclarator. Used for chain of
   *          evidence reports.
   * @param obj
   *          Node for the expression naming the object whose region is being
   *          reference.
   * @param fieldAsRegion
   *          The region being referenced.
   * @return The Set of Locks that must be held.
   */
  public Set<NeededLock> getLockForInstanceRegion(final boolean isWrite,
      final IRNode obj, final IRegion fieldAsRegion) {
    final Set<NeededLock> result = new HashSet<NeededLock>();
    getLocksFromAggregation(isWrite, obj, fieldAsRegion,
        NullSet.<Effect>prototype(), false, result,
        NullSet.<AggregationEvidence>prototype());
    return result;
  }

  /**
   * Given a reference to an instance region, find the locks that protect that
   * region, using aggregation information.
   * 
   * @param src
   *          The reference to the region, either a FieldRef,
   *          ArrayRefExpression, or VariableDeclarator. Used for chain of
   *          evidence reports.
   * @param obj
   *          Node for the expression naming the object whose region is being
   *          reference.
   * @param fieldAsRegion
   *          The region being referenced.
   * @param skipSelf
   *          Whether the initial field access should be included in the
   *          accesses that need locks or not.
   * @param result
   *          The Set to which the needed locks will be added.
   */
  private final void getLocksFromAggregation(final boolean isWrite,
      final IRNode obj, final IRegion fieldAsRegion, 
      final Set<Effect> conflicts, final boolean skipSelf,
      final Set<NeededLock> result, final Set<AggregationEvidence> outEvidence) {
    /*
     * Field may belong to other targets because of (uniqueness) aggregation.
     * Find those targets and see if any of them are protected.
     * 
     * Get all the regions that the region aggregates into. Each level of
     * aggregation may have a lock associated with it. We require each of those
     * locks to be held. Having multiple locks for a region is, in general, bad,
     * but it is okay here because of uniqueness. That is, the multiple levels
     * of locking are redundant.
     * 
     * TODO: In the future change to only use the outermost level of locking.
     * Don't do this yet, because there are several other changes that need to
     * be made along with it. See long winded e-mail from mid-May 2003.
     */
    final List<Target> targets =
      AggregationUtils.fieldRefAggregatesInto(binder, targetFactory, obj, fieldAsRegion);
    final Iterator<Target> iter = targets.iterator();
    // Skip the first target (the field reference itself) if needed
    if (skipSelf) {
      @SuppressWarnings("unused")
      final Object junk = iter.next();
    }
    while (iter.hasNext()) {
      final Target testTgt = iter.next();
      final IRegion testRegion = testTgt.getRegion();
      if (testRegion.isStatic()) {
        final IRNode cdecl = VisitUtil.getClosestType(testRegion.getNode());
        final RegionLockRecord neededLock =
          getLockForRegion(JavaTypeFactory.getMyThisType(cdecl), testRegion);
        if (neededLock != null) {
          // Static region must be protected by a static lock
          /* Whether field is being written to determines whether we need a read
           * or write lock.
           */ 
          final NeededLock l =
            neededLockFactory.createStaticLock(neededLock.lockDecl, isWrite);
          result.add(l);
          final AggregationEvidence ev = new AggregationEvidence(
              obj, fieldAsRegion, conflicts, testRegion, l);
          outEvidence.add(ev);
        }
      } else {
        final IRNode ref = testTgt.getReference();
        final IJavaType jt = binder.getJavaType(ref);
        // Arrays aren't classes
        if (jt instanceof IJavaDeclaredType) {
          final RegionLockRecord neededLock =
            getLockForRegion((IJavaDeclaredType) jt, testRegion);
          if (neededLock != null) {
            final NeededLock l;
            if (neededLock.lockDecl.isLockStatic()) {
              /* Whether field is being written to determines whether we need a read
               * or write lock.
               */ 
              l = neededLockFactory.createStaticLock(neededLock.lockDecl, isWrite);
            } else {
              /* Whether field is being written to determines whether we need a read
               * or write lock.
               */ 
              l = neededLockFactory.createInstanceLock(
                  ref, neededLock.lockDecl, isWrite);
            }
            result.add(l);
            final AggregationEvidence ev = new AggregationEvidence(
                obj, fieldAsRegion, conflicts, testRegion, l);          
            outEvidence.add(ev);
          }
        }
      }
    }
  }

  // we already know that mcall is an invocation of an instance method!
  /**
   * A method call may require locks because the regions it affects are
   * aggregated into regions that are protected in the referring object.
   * Specifically, if an actual parameter (including the receiver) is a field
   * reference to an unique field, then the effects the method has on regions of
   * that field must be reinterpreted based on the field's aggregation mapping,
   * and the associated locks looked up. This method returns those locks that
   * need to be held because of this.
   * 
   * @param mcall
   *          A MethodCall, ConstructorCall, NewExpression, or
   *          AnonClassExpression
   * @param enclosingDecl
   *          The constructor/method decl of the constructor/method that contains mcall
   */
  public Set<NeededLock> getLocksForMethodAsRegionRef(
      final IRNode mcall, final IRNode enclosingDecl) {
    final Set<NeededLock> result = new HashSet<NeededLock>();
    
    /* Get the effects of calling the method, and find all the effects
     * whose targets are the result of aggregation.  For each such target
     * get the lock required to access the region into which the aggregation
     * occurred.
     */
    /* We use the static method here because we already have a referenced to the
     * enclosing method declaration that contains the method call from our
     * caller (it is needed for other things in our caller). Using the instance
     * method would cause an unneeded crawl up the parse tree to find the
     * enclosing method declaration.
     */
    final Set<Effect> callFx = EffectsVisitor.getMethodCallEffects(
        effectsVisitor.getBCA(), targetFactory, binder, mcall, enclosingDecl);
    for (final Effect effect : callFx) {
      if (effect.isTargetAggregated()) {
        getLocksFromEffect(effect, result);
      }
    }
    return Collections.unmodifiableSet(result);
  }

  /**
   * Given an effect, add the needed locks (if any) for the accessed region to
   * the set of locks.
   * 
   * <p>
   * Currently only works with effects referencing class targets and instance
   * targets. <em>Does not do anything any instance targets; simply ignores
   * them.</em>
   */
  private void getLocksFromEffect(
      final Effect effect, final Set<NeededLock> result) {
    /* If the target comes from aggregation, it is possible that the region
     * that has been aggregated has subregions that are aggregated into
     * lock-protected regions.  This is not immediately apparent from looking 
     * at the region of the original target.  This kind of thing should only
     * happen when dealing with effects that come from method calls, where the
     * declared effects of the method obscure the actual fields and regions
     * touched by the method implementation.
     * 
     * We always check the original target as well.  If the additional regions
     * are protected by locks, then the original target is not.
     */
    final Target target = effect.getTarget();
    final Set<Target> targets = new HashSet<Target>();
    targets.add(target);
    final com.surelogic.analysis.effects.AggregationEvidence aggEvidence = target.getLastAggregation();
    if (aggEvidence != null) { // We have aggregation
      final IRegion aggedRegion = aggEvidence.getOriginalRegion();
      final Map<RegionModel, IRegion> aggMap = aggEvidence.getRegionMapping();
      for (final Map.Entry<RegionModel, IRegion> mapping : aggMap.entrySet()) {
        if (aggedRegion.ancestorOf(mapping.getKey())) {
          final IRegion destRegion = mapping.getValue();
          if (destRegion.isStatic()) {
            targets.add(targetFactory.createClassTarget(destRegion));
          } else {
            final IRNode objExpr;
            if (target instanceof ClassTarget) {
              objExpr = FieldRef.getObject(aggEvidence.getOriginalExpression());
            } else {
              objExpr = target.getReference();
            }
            targets.add(targetFactory.createInstanceTarget(objExpr, destRegion, target.getElaborationEvidence()));
          }
        }
      }
    }
    
    final boolean isWrite = effect.isWriteEffect();
    for (Target t : targets) {
      /* BCA only helps us if it yields a FieldRef expression that can then be
       * used with aggregation.  If aggregation doesn't occur after a BCA,
       * then we don't care about the result.  We take our given target and 
       * backtrack over BCA until we hit aggregation or the end.
       */
      t = t.undoBCAElaboration();
      
      final IRegion region = t.getRegion();
      /* Final and volatile regions do not need locks */
      if (!region.isFinal() && !region.isVolatile()) {
        final IJavaType lookupRegionInThisType = t.getRelativeClass(binder);
        /* Arrays aren't classes --- Not sure why this would happen,
         * but it was a problem in the past. 
         */
        if (lookupRegionInThisType instanceof IJavaDeclaredType) {
          final RegionLockRecord neededLock =
            getLockForRegion((IJavaDeclaredType) lookupRegionInThisType, region);
          if (neededLock != null) {
            if (t instanceof ClassTarget) {
              final NeededLock l =
                neededLockFactory.createStaticLock(neededLock.lockDecl, isWrite);
              result.add(l);
            } else { // InstanceTarget
              final NeededLock l;
              if (neededLock.lockDecl.isLockStatic()) {
                l = neededLockFactory.createStaticLock(neededLock.lockDecl, isWrite);
              } else {                
                l = neededLockFactory.createInstanceLock(
                    t.getReference(), neededLock.lockDecl, isWrite);
              }
              result.add(l);
            }
          }
        }
      }
    }
  }

  /**
   * ((( Come Back to This ))) Get the locks that must be held to make the given
   * method call.
   * 
   * @param mcall
   *          A MethodCall, ConstructorCall, NewExpression, or
   *          AnonClassExpression node.
   * @param callingDecl
   *          The method or constructor declaration node of the method/constructor
   *          that contains the method call mcall.
   * @return A set of Targets whose locks must be held.
   */
  public GoodAndBadLocks<NeededLock> getLocksForMethodCall(
      final IRNode mcall, final IRNode callingDecl) {
    final GoodAndBadLocks<NeededLock> locks = new GoodAndBadLocks<NeededLock>();
    final IRNode mdecl = binder.getBinding(mcall);
    if (mdecl == null) {
      return locks;
    }
    final RequiresLockPromiseDrop reqLockD = LockRules.getRequiresLock(mdecl);
    if (reqLockD == null) {
      return locks;
    }
    final List<LockSpecificationNode> lockNames = reqLockD.getAST().getLockList();
    if (!lockNames.isEmpty()) {
      final Map<IRNode, IRNode> m =
        MethodCallUtils.constructFormalToActualMap(binder, mcall, mdecl, callingDecl);

      // Now, build the set of locks by substituting actuals for formals
      for(final LockSpecificationNode ln : lockNames) {
        final NeededLock lock =
          convertNeededLockNameToCallerContext(mdecl, ln, m);
        if (lock != null) locks.goodLocks.add(lock);
        else locks.badLocks.add(ln);
      }
    }
    return locks;
  }

  
  
  // ========================================================================
  // == Methods for converting lock expressions into locks
  // ========================================================================

  /**
   * Convert an expression known to refer to a java.util.concurrent.locks.Lock
   * object into a set of declared locks.
   * 
   * @param lockExpr
   *          An expression known to refer to a java.util.concurrent.locks.Lock
   *          instance.
   * @param src
   * @return
   */
  public void convertJUCLockExpr(
      final IRNode lockExpr, final IRNode enclosingDecl, final IRNode src, final Set<HeldLock> lockSet) {
    /* We start by assuming we will be creating a Lock object, and not a
     * ReadWriteLockObject; thus, we set isWrite to true and isRW to false.
     */
    convertJUCLockExpr(lockExpr, enclosingDecl, true, false, src, lockSet);
  }

  /**
   * Note: A lock expression always results in a non-{@link HeldLock#isAssumed() assumed} lock.
   * 
   */
  private void convertJUCLockExpr(
      final IRNode lockExpr, final IRNode enclosingDecl, 
      final boolean isWrite, final boolean isRW,
      final IRNode src, final Set<HeldLock> lockSet) {
    final Operator op = JJNode.tree.getOperator(lockExpr);

    /* For method calls that return the declared lock, if any */
    if (MethodCall.prototype.includes(op)) {
      /* We need to see if the method is a call to readLock or writeLock, and
       * handle it accordingly.
       */
      final ReadWriteLockMethods whichMethod = whichReadWriteLockMethod(lockExpr);
      if (whichMethod == ReadWriteLockMethods.READLOCK ||
          whichMethod == ReadWriteLockMethods.WRITELOCK) {
        // Dealing with a read-write lock
        final boolean newIsWrite = whichMethod == ReadWriteLockMethods.WRITELOCK;
        final MethodCall mcall = (MethodCall) op;
        convertJUCLockExpr(mcall.get_Object(lockExpr), enclosingDecl, newIsWrite, true, src, lockSet);
      } else {
        // Dealing with a normal lock: it must be a write lock
        final HeldLock returnedLock =
          this.convertReturnedLock(lockExpr, enclosingDecl, isWrite, src);
        if (returnedLock != null) {
          lockSet.add(returnedLock);
        } else {
          // method doesn't return a known lock, so nothing to do.
        }
      }
    } else {
      /* NB. "this" and ".class" cannot be java.util.concurrent.locks.Lock
       * objects, so we skip those lines of processing.  Basically, we are only
       * interested in pure field accesses.
       */
      if (FieldRef.prototype.includes(op)) { // lockExpr == 'e.f'
        final IRNode obj = FieldRef.getObject(lockExpr);
        final IJavaType objType = binder.getJavaType(obj);
        // Arrays cannot declare locks
        if (objType instanceof IJavaDeclaredType) {
          final IRNode potentialLockImpl = this.binder.getBinding(lockExpr);

          // see if 'f' is a lock in class typeOf(e)
          // reminder: lockExpr is a FieldRef; binding it gives the field decl
          final Set<AbstractLockRecord> records =
            sysLockModelHandle.get().getRegionAndPolicyLocksForLockImpl(
                (IJavaDeclaredType) objType, potentialLockImpl);
          if (TypeUtil.isStatic(potentialLockImpl)) {
            for (final AbstractLockRecord lr : records) {
              final HeldLock lock;
              if (isRW) {
                lock = heldLockFactory.createJUCRWStaticLock(lr.lockDecl, src, false, isWrite);
              } else {
                lock = heldLockFactory.createJUCStaticLock(lr.lockDecl, src, false);
              }
              lockSet.add(lock);
            }
          } else {
            for (final AbstractLockRecord lr : records) {
              // If we only have a fieldRef, then it must a Lock and not a ReadWriteLock, so it is write lock
              final HeldLock lock;
              if (isRW) {
                lock = heldLockFactory.createJUCRWInstanceLock(obj, lr.lockDecl, src, null, false, isWrite);
              } else {
                lock = heldLockFactory.createJUCInstanceLock(obj, lr.lockDecl, src, null, false);
              }
              lockSet.add(lock);
            }
          }
          
          /* lockExpr = 'e.f', continued.  Here we try to support a coding
           * idiom for ReadWriteLocks where the separate read and write lock
           * references are cached into fields, e.g.,
           *   final ReadWriteLock rwLock = ...
           *   final Lock rLock = rwLock.readLock();
           *   final Lock wLock = rwLock.writeLock();
           * 
           * INSTANCE CASE: See if the field 'f' is an final field initialized
           * with either ReadWriteLock.readLock() or ReadWriteLock.writeLock().
           * Must be of the form "f = this.g.readLock()" or "this.g.writeLock()",
           * where 'g' is a final field.  Things like "this.g.h.readLock()" or
           * "c.readLock()" are no good because we need to be able to understand
           * the expression in different contexts.
           * 
           * STATIC CASE: See if the field 'f' is a final static field initialized
           * with either ReadWriteLock.readLock() or ReadWriteLock.writeLock().
           * Must be of the form 'f = C.g.readLock()' or 'f = C.g.writeLock',
           * where 'g' is a final static field declared in class C, and 'f' is
           * also declared in class C.
           */
          if (TypeUtil.isFinal(potentialLockImpl)) { // Final field, check initialization
            final IRNode init = VariableDeclarator.getInit(potentialLockImpl);
            if (Initialization.prototype.includes(init)) {
              final IRNode initValue = Initialization.getValue(init);
              final Operator initValueOp = JJNode.tree.getOperator(initValue);
              if (MethodCall.prototype.includes(initValueOp)) {
                final ReadWriteLockMethods whichMethod = whichReadWriteLockMethod(initValue);
                if (whichMethod == ReadWriteLockMethods.READLOCK ||
                    whichMethod == ReadWriteLockMethods.WRITELOCK) {
                  /* We have 'f = e.readLock()' or 'f = e.writeLock()' */
                  final boolean newIsWrite = whichMethod == ReadWriteLockMethods.WRITELOCK;
                  final IRNode mcObject = ((MethodCall) initValueOp).get_Object(initValue);
                  final Operator mcObjectOp = JJNode.tree.getOperator(mcObject);
                  if (FieldRef.prototype.includes(mcObjectOp)) {
                    // We have 'f = e.g.readLock()' or 'f = e.g.writeLock()'
                    final IRNode mcBoundField = binder.getBinding(mcObject);
                    // Field 'g' must be final
                    if (TypeUtil.isFinal(mcBoundField)) {
                      if (TypeUtil.isStatic(mcBoundField)) {
                        // Check that 'f' and 'g' are both declared in the same class
                        if (TypeUtil.isStatic(potentialLockImpl)) {
                          final IRNode fClassBody = VisitUtil.getClosestType(potentialLockImpl);
                          final IRNode gClassBody = VisitUtil.getClosestType(mcBoundField);
                          if (fClassBody.equals(gClassBody)) {
                            final Set<AbstractLockRecord> records2 =
                              sysLockModelHandle.get().getRegionAndPolicyLocksForLockImpl(
                                  (IJavaDeclaredType) objType, mcBoundField);
                            for (final AbstractLockRecord lr : records2) {
                              lockSet.add(
                                  heldLockFactory.createJUCRWStaticLock(
                                      lr.lockDecl, src, false, isWrite));
                            }
                          }
                        }
                      } else {
                        // Check that the FieldRef is "this.g"
                        final IRNode frObject = FieldRef.getObject(mcObject);
                        final Operator frObjectOp = JJNode.tree.getOperator(frObject);
                        if (ThisExpression.prototype.includes(frObjectOp)) {
                          // We have 'f = this.g.readLock()' or 'f = this.g.writeLock'
                          final IJavaType frObjectType = binder.getJavaType(frObject);
                          if (frObjectType instanceof IJavaDeclaredType) { // sanity check
                            // see if 'g' is a lock in class typeOf(this)
                            final Set<AbstractLockRecord> records2 =
                              sysLockModelHandle.get().getRegionAndPolicyLocksForLockImpl(
                                  (IJavaDeclaredType) frObjectType, mcBoundField);
                            for (final AbstractLockRecord lr : records2) {
                              /* NB. Here we use the 'obj', the receiver from
                               * the original FieldRef above! We are doing an
                               * alpha-renaming of the original receiver
                               * expression for 'this'.
                               */
                              lockSet.add(
                                  heldLockFactory.createJUCRWInstanceLock(
                                      obj, lr.lockDecl, src,
                                      null, false, newIsWrite));
                            }
                          }
                        }                      
                      }
                    }
                  }
                }                
              }
            }
          }
        }
      }
    }
  }

  
  
  // ========================================================================
  // == Methods for converting lock names into locks
  // ========================================================================

  /**
   * Given a method call, see if the method is declared to return a lock, 
   * and if so, convert the lock named in the
   * {@code @returnsLock} annotation into a Lock.
   * 
   * @param mcall
   *          The MethodCall node to process.
   * @param callingDecl
   *          The constructor or method declaration node of the method/constructor
   *          that contains mcall
   * @param src
   *          The source statement for the generated lock
   * @return The Lock named by the annotation or <code>null</code> if the
   *         method call does not result in a lock.
   */
  // Converts returned lock for a caller of the method with the annotation
  public HeldLock convertReturnedLock(
      final IRNode mcall, final IRNode callingDecl, final IRNode src) {
    return convertReturnedLock(mcall, callingDecl, true, src);
  }

  // Converts returned lock for a caller of the method with the annotation
  // callingDecl is the method/constructor that contains mcall
  private HeldLock convertReturnedLock(
      final IRNode mcall, final IRNode callingDecl,
      final boolean isWrite, final IRNode src) {
    // See if the method even returns a lock
    final IRNode mdecl                        = binder.getBinding(mcall);
    final ReturnsLockPromiseDrop returnedLock = LockUtils.getReturnedLock(mdecl);
    if (returnedLock != null) {
      final Map<IRNode, IRNode> m = MethodCallUtils.constructFormalToActualMap(
          binder, mcall, mdecl, callingDecl);
      return convertHeldLockNameToCallerContext(
          mdecl, returnedLock.getAST().getLock(), isWrite, src, m);
    } else {
      return null;
    }
  }

  /**
   * Given a LockSpecificationNode from a {@code ReturnsLock} or
   * {@code RequiresLock} annotation on method {@code mdecl}, convert it into a
   * lock that can used inside the method implementation.
   * 
   * @param mdecl
   *          The method whose annotation the LockName comes from.
   * @param lockSpec
   *          The lock specification to convert into a lock object.
   * @param isAssumed
   *          Should the lock be assumed to be held; That is, are we
   *          processing locks from a lock precondition?
   * @param supportingDrop
   *          The drop that gives evidence for this lock: must be non-{@code null}
   *          if the lock specification came from a {@code RequiresLock}; must
   *          be {@code null} if the lock specification came from a
   *          {@code ReturnsLock}.
   * @param formalRcvr
   *          The ReceiverDeclaration node associated with {@code mdecl} or
   *          {@code null} if the method is static. (The implementation could
   *          get this value from {@code mdecl}, but there are some cases where
   *          the caller would have had this information already, so it is silly
   *          to refetch it here.)
   * 
   * @return The lock object that represents the named lock.
   */
  public HeldLock convertLockNameToMethodContext(final IRNode mdecl,
      final LockSpecificationNode lockSpec, final boolean isAssumed,
      final RequiresLockPromiseDrop supportingDrop,
      final IRNode formalRcvr) {
    final IRNode src = lockSpec.getPromisedFor();
    final LockModel lockDecl = lockSpec.resolveBinding().getModel();
    final LockNameNode lockName = lockSpec.getLock();
    final boolean isWriteLock = lockSpec.getType() != LockType.READ_LOCK;
    
    if (lockDecl.isLockStatic()) {
      return heldLockFactory.createStaticLock(lockDecl, src, supportingDrop, isAssumed, isWriteLock);
    } else {
      if (lockName instanceof SimpleLockNameNode) {
        // Lock is "this.<LockName>"
        return heldLockFactory.createInstanceLock(formalRcvr, lockDecl, src, supportingDrop, isAssumed, isWriteLock);
      } else { // QualifiedLockNameNode
        final ExpressionNode base = ((QualifiedLockNameNode) lockName).getBase();
        if (base instanceof TypeExpressionNode) {
          // Should be handled in the "isLockStatic" case above!
          throw new IllegalStateException("Lock should be static, but isLockStatic() is false");
        } else if (base instanceof ThisExpressionNode) {
          // Lock is "this.<LockName>"
          return heldLockFactory.createInstanceLock(formalRcvr, lockDecl, src, supportingDrop, isAssumed, isWriteLock);
        } else if (base instanceof QualifiedThisExpressionNode) {
          final QualifiedThisExpressionNode qthis = (QualifiedThisExpressionNode) base;
          final IRNode canonicalQThis =
            JavaPromise.getQualifiedReceiverNodeByName(
                mdecl, qthis.getType().resolveType().getNode());
          // Lock is "x.y.Z.this.<LockName>"
          return heldLockFactory.createInstanceLock(canonicalQThis, lockDecl, src, supportingDrop, isAssumed, isWriteLock);
        } else {
          // Lock is "<UseExpression>.<LockName>"
          return heldLockFactory.createInstanceLock(
              base, lockDecl, src, supportingDrop, isAssumed, isWriteLock);
        }
      }
    }
  }
  
  /**
   * Given a LockSpecification from a <code>requiresLock</code>, convert it into the
   * lock object that is <em>needed</em> in the calling context. This involved
   * replacing formal parameter names with actual argument expressions.
   * 
   * @param lock
   *          A LockSpecification node from a <code>requiresLock</code> or
   *          <code>returnsLock</code> annotation.
   * @param formalRcvr
   *          The node representing the formal receiver for the method; used to
   *          look up the actual receiver expression in <code>map</code>
   * @param map
   *          The map from formals to actuals, including a mapping for the
   *          receiver.
   * @return The Lock named by the annotation, or <code>null</code> if the lock
   * cannot be named in the calling context.
   */
  public NeededLock convertNeededLockNameToCallerContext(
      final IRNode mdecl, final LockSpecificationNode lockSpec,
      final Map<IRNode, IRNode> map) {
    final LockModel lockModel = lockSpec.resolveBinding().getModel();
    final LockNameNode lockName = lockSpec.getLock();
    final boolean isWriteLock = lockSpec.getType() != LockType.READ_LOCK;
    if (lockModel.isLockStatic()) {
      return neededLockFactory.createStaticLock(lockModel, isWriteLock);
    } else {
      final IRNode objExpr = 
        convertObjectExpressionToCallerContext(mdecl, lockName, map);
      if (objExpr != null) {
        return neededLockFactory.createInstanceLock(
            objExpr, lockModel, isWriteLock);
      } else {
        return null;
      }
    }
  }
  
  /**
   * Given a LockName from a <code>returnsLock</code> annotation, convert it
   * into the lock object that will be <em>held</em> in the calling context.
   * This involved replacing formal parameter names with actual argument
   * expressions.
   * 
   * @param lock
   *          A LockName node <code>returnsLock</code> annotation.
   * @param src
   *          The expression blamed for the construction of the lock
   * @param formalRcvr
   *          The node representing the formal receiver for the method; used to
   *          look up the actual receiver expression in <code>map</code>
   * @param map
   *          The map from formals to actuals, including a mapping for the
   *          receiver.
   * @return The Lock named by the annotation
   */
  public HeldLock convertHeldLockNameToCallerContext(
      final IRNode mdecl, final LockNameNode lockName, final boolean isWrite,
      final IRNode src, final Map<IRNode, IRNode> map) {
    final LockModel lockModel = lockName.resolveBinding().getModel();  
    if (lockModel.isLockStatic()) {
      return heldLockFactory.createStaticLock(lockModel, src, null, false, isWrite);
    } else {
      final IRNode objExpr =
        convertObjectExpressionToCallerContext(mdecl, lockName, map);
      return heldLockFactory.createInstanceLock(objExpr, lockModel, src, null, false, isWrite);
    }
  }

  private IRNode convertObjectExpressionToCallerContext(
      final IRNode mdecl, final LockNameNode lock, final Map<IRNode, IRNode> map) {
    IRNode objExpr = null;
    if (lock instanceof SimpleLockNameNode) {
      // XXX: What about unqualified static fields?
      // Lock is "this.<LockName>"
      objExpr = map.get(JavaPromise.getReceiverNodeOrNull(mdecl));
    } else { // QualifiedLockNameNode
      final ExpressionNode base = ((QualifiedLockNameNode) lock).getBase();
      if (base instanceof ThisExpressionNode) {
        // Lock is "this.<LockName>"
        objExpr = map.get(JavaPromise.getReceiverNodeOrNull(mdecl));
      } else if (base instanceof QualifiedThisExpressionNode) {
        // Lock is "Class.this.<LockName>"
        final QualifiedThisExpressionNode qthis = (QualifiedThisExpressionNode) base;
        final IRNode qrcvr = JavaPromise.getQualifiedReceiverNodeByName(mdecl, qthis.getType().resolveType().getNode());
        objExpr = map.get(qrcvr);
      } else {
        VariableUseExpressionNode use = (VariableUseExpressionNode) base;
        
        // Lock is "<UseExpression>.<LockName>"
        objExpr = map.get(use.resolveBinding().getNode());
      }
    }
    return objExpr;
  }
//
//  /**
//   * Build a map from formals to Actuals for a method call. Now also used in
//   * ColorTargets.
//   * 
//   * @param mcall
//   *          The node for the invocation expression
//   * @param mdecl
//   *          The node for the method declaration
//   * @return <code>Map</code> from formals to Actuals
//   */
//  private Map<IRNode, IRNode> constructFormalToActualMap(
//      final IRNode mcall, final IRNode mdecl) {
//    // get the formal parameters
//    final IRNode params = ((SomeFunctionDeclaration) JJNode.tree.getOperator(mdecl)).get_Params(mdecl);
//    final Iterator<IRNode> paramsEnum = Parameters.getFormalIterator(params);
//
//    // get the actual parameters
//    final IRNode actuals = ((CallInterface) JJNode.tree.getOperator(mcall)).get_Args(mcall);
//    final Iterator<IRNode> actualsEnum = Arguments.getArgIterator(actuals);
//
//    // build a table mapping each formal parameter to its actual
//    final Map<IRNode, IRNode> table = new HashMap<IRNode, IRNode>();
//    while (paramsEnum.hasNext()) {
//      table.put(paramsEnum.next(), actualsEnum.next());
//    }
//
//    /* mcall could be a an AnonClassExpression, ConstructorCall, MethodCall,
//     * or NewExpression.  If it is a call to a non-static method, we must map
//     * the receiver, and in some cases we can map the qualified receivers.  If
//     * it is a ConstructorCall (i.e., "this(...)" or "super(...)", we must map
//     * the receiver, and in some cases we can map the qualified receivers.
//     * Otherwise, we do not have enough information to map the receivers.
//     */
//    final Operator op = JJNode.tree.getOperator(mcall);
//    if ((MethodCall.prototype.includes(op) && !TypeUtil.isStatic(mdecl)) ||
//        ConstructorCall.prototype.includes(op)) {
//      final boolean mapQualifiedReceivers;
//      if (ConstructorCall.prototype.includes(op)) {
//        mapQualifiedReceivers = true;
//      } else {
//        final IRNode actualRcvr = ((SomeFunctionCall) op).get_Object(mcall);
//        final Operator actualRcvrOp = JJNode.tree.getOperator(actualRcvr);
//        table.put(JavaPromise.getReceiverNodeOrNull(mdecl), actualRcvr);
//        mapQualifiedReceivers = 
//          ThisExpression.prototype.includes(actualRcvrOp) ||
//          SuperExpression.prototype.includes(actualRcvrOp);
//      }
//      if (mapQualifiedReceivers) {
//        /* Special case: we can map Qualified receivers of the called method
//         * to those of the calling context.
//         */
//        final IRNode callingMethodDecl = VisitUtil.getEnclosingMethod(mcall);
//        
//        // Get the qualified receivers of the called method
//        for (final IRNode qrn : JavaPromise.getQualifiedReceiverNodes(mdecl)) {
//          final IRNode type = QualifiedReceiverDeclaration.getType(binder, qrn);
//          // Get the corresponding qualified receiver in the calling context
//          final IRNode callingQRN = JavaPromise.getQualifiedReceiverNodeByName(callingMethodDecl, type);
//          table.put(qrn, callingQRN);
//        }
//      }
//    }
//    return Collections.unmodifiableMap(table);
//  }
  
  public RegionModel getElementRegion() {
    return elementRegion;
  }
  
  public InstanceTarget createInstanceTarget(
      final IRNode object, final IRegion region) {
    return targetFactory.createInstanceTarget(object, region);
  }

  public ClassTarget createClassTarget(final IRegion field) {
    return targetFactory.createClassTarget(field);
  }  
}
