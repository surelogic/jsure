package com.surelogic.analysis.concurrency.heldlocks;

import com.surelogic.aast.java.*;
import com.surelogic.aast.promise.*;
import com.surelogic.analysis.MethodCallUtils;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.concurrency.heldlocks.locks.HeldLock;
import com.surelogic.analysis.concurrency.heldlocks.locks.HeldLockFactory;
import com.surelogic.analysis.concurrency.heldlocks.locks.ILock;
import com.surelogic.analysis.concurrency.heldlocks.locks.NeededLock;
import com.surelogic.analysis.concurrency.heldlocks.locks.NeededLockFactory;
import com.surelogic.analysis.concurrency.heldlocks.locks.ILock.Type;
import com.surelogic.analysis.effects.*;
import com.surelogic.analysis.effects.targets.AggregationEvidence;
import com.surelogic.analysis.effects.targets.AnyInstanceTarget;
import com.surelogic.analysis.effects.targets.ClassTarget;
import com.surelogic.analysis.effects.targets.DefaultTargetFactory;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.NoEvidence;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetFactory;
import com.surelogic.analysis.effects.targets.ThisBindingTargetFactory;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.*;
import com.surelogic.common.logging.SLLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.ArrayRefExpression;
import edu.cmu.cs.fluid.java.operator.CastExpression;
import edu.cmu.cs.fluid.java.operator.CharLiteral;
import edu.cmu.cs.fluid.java.operator.ClassExpression;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.IntLiteral;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.ParenExpression;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
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
 * <p><em>I HATE THIS CLASS.  IT NEEDS TO BE MADE A STATIC UTILITY CLASS,
 * OR SPLIT UP INTO SEVERAL SMALLER HELPER CLASSES.</em>
 * 
 * @author aarong
 */
public final class LockUtils {
  public static enum HowToProcessLocks {
    INTRINSIC {
      @Override
      public boolean acceptsLock(final LockModel lm) {
        return !lm.isJUCLock();
      }
      
      @Override
      public ILock.Type getAssumedLockType(final LockModel lm) {
        return Type.MONOTLITHIC;
      }
    },
    
    JUC {
      @Override
      public boolean acceptsLock(final LockModel lm) {
        return lm.isJUCLock();
      }      
      
      @Override
      public ILock.Type getAssumedLockType(final LockModel lm) {
        return lm.isReadWriteLock() ? Type.WRITE : Type.MONOTLITHIC;
      }
    };
  
    public abstract boolean acceptsLock(LockModel lm);
    
    /**
     * Given a lock that analysis assumes to be held because of constructor
     * or initializer semantics, what type should be assigned to the lock.
     */
    public abstract ILock.Type getAssumedLockType(LockModel lm);
  }
  
  
  
  /**
   * See if a given expression is a "final expression," that is an expression
   * whose value is fixed no matter where in the synchronized block it is 
   * evaluated.
   * 
   * @param expr
   *          The expression to test
   */
  public interface FinalExpressionChecker {
    public boolean isFinal(IRNode expr);
  }
  
  
  
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
   * Name of the wait-queue lock defined for <code>java.lang.Object</code>
   * used by the {@link java.lang.Object#wait()}method, etc.
   */
  public static final String MUTEX_NAME = "MUTEX"; //$NON-NLS-1$
  
  /** Reference to the Instance region */
  //private final RegionModel INSTANCE;

  

  /**
   * The model of all the locks in the system.
   * @see LockVisitor#sysLockModelHandle
   */
  private AtomicReference<GlobalLockModel> sysLockModelHandle;

  /** The Java name binder to use. */
  private final IBinder binder;
  
  /** The effects analysis to use. */
  private final Effects effects;
  
  /** The alias analyis to use. */
  private final IMayAlias mayAlias;
  
  /** Factory for creating needed locks */
  private final NeededLockFactory neededLockFactory;
  
  /** Factory for creating targets */
  private final TargetFactory targetFactory;

  /**
   * Reference to the lock declaration node of the MUTEX lock defined in
   * <code>java.lang.Object</code> that is used as the precondition for
   * {@link java.lang.Object#wait()}and friends.
   */
  private final LockModel mutex;

  /** The element region: {@code []}. */
  //private final RegionModel elementRegion;
  
  /**
   * The internal representation of the {@link java.util.concurrent.locks.Lock}
   * interface.  We look this up once in the type environment during 
   * construction to avoid repeated lookups.
   */
  private final IJavaType lockType;
  
  /**
   * The internal representation of the {@link java.util.concurrent.locks.ReadWriteLock}
   * interface.  We look this up once in the type environment during 
   * construction to avoid repeated lookups.
   */
  private final IJavaType readWriteLockType;

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
      final IBinder b, final Effects e, final IMayAlias ma,
      final NeededLockFactory nlf,
      final ThisExpressionBinder thisExprBinder) {
    sysLockModelHandle = glmRef;
    binder = b;
    effects = e;
    mayAlias = ma;
    neededLockFactory = nlf;
    targetFactory = new ThisBindingTargetFactory(thisExprBinder);
    
    if (binder == null || binder.getTypeEnvironment() == null) {
    	throw new IllegalStateException();
    }
    lockType = JavaTypeFactory.convertNodeTypeToIJavaType(
          binder.getTypeEnvironment().findNamedType(JAVA_UTIL_CONCURRENT_LOCKS_LOCK),
          binder);
    readWriteLockType = JavaTypeFactory.convertNodeTypeToIJavaType(
        binder.getTypeEnvironment().findNamedType(JAVA_UTIL_CONCURRENT_LOCKS_READWRITELOCK),
        binder);

    // Get the instance region declaration
    //INSTANCE = RegionModel.getInstanceRegion();
    
    // Get the lock decl of the MUTEX lock on Object
    final RegionLockRecord lr = sysLockModelHandle.get().getRegionLockByName(
        binder.getTypeEnvironment().getObjectType(), MUTEX_NAME);
    if (lr == null) {
      mutex = null; 
    } else {
      mutex = lr.lockDecl;
    }
    // Make sure the MUTEX lock shows up in the viewer
    // XXX: NullPointerException if the lock is not found. This is okay because it is catastrophic if MUTEX is not decalred
    mutex.setFromSrc(true);
  }

  synchronized void clear() {
    subTypeCache.clear();
  }

  // ========================================================================
  // == Test for final expressions
  // ========================================================================

  /**
   * Get a check object that can test if expressions in the given flow unit are
   * "final." An expression is "final" if its value is fixed no matter where in
   * the synchronized block it is evaluated.
   * @param block
   *          The block whose lock expression contains
   *          <code>expr</code>. May be <code>null</code> if the expression is
   *          not part of the lock expression of a synchronized block.
   */
  public FinalExpressionChecker getFinalExpressionChecker(
      final BindingContextAnalysis.Query bcaQuery,
      final IRNode flowUnit, final IRNode block) {
    /*
     * TODO Need to modify this method to produce
     * a nested chain of evidence of why it believes an expression to NOT be a
     * final expression. Chain should contain links to the field/var declaration
     * showing that the variable is not declared to be final, etc.
     */
    return new FinalExpressionChecker() {
      final Effects.Query fxQuery = effects.getEffectsQuery(flowUnit, bcaQuery);
      final ConflictChecker conflicter = new ConflictChecker(binder, mayAlias);
      
      public boolean isFinal(final IRNode expr) {
        final Operator op = JJNode.tree.getOperator(expr);
        if (CastExpression.prototype.includes(op)) {
          // Final if the nested expression is final
          return isFinal(CastExpression.getExpr(expr));
        } else if (ParenExpression.prototype.includes(op)) {
          // Final if the nested expression is final
          return isFinal(ParenExpression.getOp(expr));
        } else if (MethodCall.prototype.includes(op)) {
          MethodCall mcall = (MethodCall) op;
          /* Object expression must be final or method must be static, and the method
           * must have a @returnsLock annotation (which we are using as a very
           * specific idempotency annotation) or a call to readLock() or writeLock().
           */
          final IRNode mdecl = binder.getBinding(expr);
          if (TypeUtil.isStatic(mdecl)
              || isFinal(mcall.get_Object(expr))) {
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
            return !isLockExpressionChangedBySyncBlock(fxQuery, conflicter, expr, block);
          }
        } else if (FieldRef.prototype.includes(op)) {
          /* Field must be final (or protected by a lock and not modified by the
           * body of the synchronized block) AND either the field must be static or
           * the object expression must be final
           */
          final IRNode id = binder.getBinding(expr);

          /* Check that the object expression is final (or static) */
          if (TypeUtil.isStatic(id)
              || isFinal(FieldRef.getObject(expr))) {
            // Check if the field is final
            if (TypeUtil.isFinal(id)) {
              return true;
            } else {
              /* Check if the field is protected by a lock and is not modified by
               * the body of the synchronized block.
               */
              return getLockForFieldRef(expr) != null
                  && !isLockExpressionChangedBySyncBlock(fxQuery, conflicter, expr, block);
            }
          }
        } else if (ArrayRefExpression.prototype.includes(op)) {
          /* Array ref expression e[e']. Expressions e and e' must be final
           * expressions. The effects of the synchronized block must not conflict
           * with reading from the array.
           */
          final IRNode array = ArrayRefExpression.getArray(expr);
          final IRNode idx = ArrayRefExpression.getIndex(expr);
          if (isFinal(array) && isFinal(idx)) {
            return !isArrayChangedBySyncBlock(fxQuery, conflicter, array, block);
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
    };
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
  private boolean isLockExpressionChangedBySyncBlock(
      final Effects.Query fxQuery, final ConflictChecker conflicter,
      final IRNode expr, final IRNode sync) {
    if (sync != null) {
      final Set<Effect> bodyEffects = fxQuery.getResultFor(sync);
      final Set<Effect> exprEffects = fxQuery.getResultFor(expr);
      return conflicter.mayConflict(bodyEffects, exprEffects);
    } else {
      return true;
    }
  }

  private boolean isArrayChangedBySyncBlock(
      final Effects.Query fxQuery, final ConflictChecker conflicter, 
      final IRNode array, final IRNode sync) {
    if (sync != null) {
      final Set<Effect> exprEffects = Collections.singleton(
          Effect.newRead(null, targetFactory.createInstanceTarget(
              array, RegionModel.getInstanceRegion(sync), NoEvidence.INSTANCE)));
      final Set<Effect> bodyEffects = fxQuery.getResultFor(sync);
      return conflicter.mayConflict(bodyEffects, exprEffects);
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
	  if (s == null || t == null) {
		  return false;
	  }
	  Boolean result = subTypeCache.get(s, t);
	  if (result != null) {
		  return result.booleanValue();
	  }
	  boolean rv = binder.getTypeEnvironment().isRawSubType(s, t);
	  subTypeCache.put(s, t, rv);
	  return rv;
  }
  
  private boolean isMethodFrom(final IRNode mcall, final IJavaType testType) {
	  IBinding b = binder.getIBinding(mcall);
	  if (b == null) {
		  SLLogger.getLogger().warning("No binding for "+DebugUnparser.toString(mcall));
		  return false;
	  }
	  IJavaType context = b.getContextType();
	  if (context == null) {
		  IRNode tdecl = VisitUtil.getEnclosingType(b.getNode());
		  context = binder.getTypeEnvironment().convertNodeTypeToIJavaType(tdecl);
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
  	if (lockType == null) {
  	    // Probably running on pre-1.5 code
  		return false;
  	}
    if (type instanceof IJavaDeclaredType) {
      return binder.getTypeEnvironment().isRawSubType(type, lockType);
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
  	if (readWriteLockType == null) {
  	    // Probably running on pre-1.5 code
  		return false;
  	}
    if (type instanceof IJavaDeclaredType) {
      return binder.getTypeEnvironment().isRawSubType(type, readWriteLockType);
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
  
  public Set<NeededLock> getLocksForDirectRegionAccess(final Effects effects,
      final BindingContextAnalysis.Query bcaQuery, final IRNode rcvr, final IRNode srcNode,
      final boolean isRead, final Target target) {
    final Set<NeededLock> neededLocks = new HashSet<NeededLock>();
    getLocksForDirectRegionAccess(effects, bcaQuery, rcvr, srcNode, isRead, target, neededLocks);
    return Collections.unmodifiableSet(neededLocks);
  }

  private void getLocksForDirectRegionAccess(final Effects effects,
      final BindingContextAnalysis.Query bcaQuery, final IRNode rcvr, final IRNode srcNode,
      final boolean isRead, final Target target,
      final Set<NeededLock> neededLocks) {
    final Set<Effect> elaboratedEffects =
      effects.elaborateEffect(bcaQuery, targetFactory, binder, rcvr, srcNode, isRead, target);
    for (final Effect effect : elaboratedEffects) {
      if (!effect.isEmpty()) {
        getLocksFromEffect(effect, LastAggregationProcessor.get(effect), neededLocks);
      }
    }
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
      final IJavaType clazz, final IRegion fieldAsRegion) {
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
        binder.getJavaType(FieldRef.getObject(fieldRef)),
        RegionModel.getInstance(binder.getBinding(fieldRef)));
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
      final Effects effects, final BindingContextAnalysis.Query bcaQuery,
      final IRNode rcvr, final ConflictChecker conflicter, 
      final IRNode mcall, final IRNode enclosingDecl) {
    final Set<NeededLock> result = new HashSet<NeededLock>();
    
    /* In the case of an effect naming a static or any-instance target, we need
     * to iterate 
     *   for each actual parameter A that is syntactically a FieldRef
     *     Get the field aggregation map M, if any, for A
     *     Let CE_T be the target from CE
     *     For each key region R in M
     *       if target <A, R> overlaps with CE_T, then we need the lock for the
     *        target obtained from aggregating target <A, R>
     *
     * So we precompute the targets <A, R> that we need to test against.
     * We cannot prebuild the needed locks because it depends on whether the
     * original effect is read or write.  (We could prebuild both, I suppose.)
     */
    final List<Target> exposedTargets = new ArrayList<Target>();
    
    // XXX This call is wasteful because the call below to getMethodCallEffects() also builds this map.
    final IRNode binding = binder.getBinding(mcall);
    if (binding != null) {
    	final Map<IRNode, IRNode> m = MethodCallUtils.constructFormalToActualMap(
    			binder, mcall, binding, enclosingDecl);
    	for (final Map.Entry<IRNode, IRNode> entry : m.entrySet()) {
    		final IRNode actual = entry.getValue();
    		if (actual != null && FieldRef.prototype.includes(actual)) {
    			final IRNode fieldID = binder.getBinding(actual);
    			final Map<IRegion, IRegion> aggregationMap = 
    			    UniquenessUtils.constructRegionMapping(fieldID);
    			if (aggregationMap != null) {
    			  for (final IRegion from : aggregationMap.keySet()) {
    			    // This is okay because only instance regions can be mapped
    			    final Target testTarget = targetFactory.createInstanceTarget(
    			        actual, from, NoEvidence.INSTANCE);
    			    exposedTargets.add(testTarget);
    				}
    			}
    		}
    	}
    } else {
    	// Already produced warning
    }

    /* Get the effects of calling the method, and find all the effects
     * whose targets are the result of aggregation.  For each such target
     * get the lock required to access the region into which the aggregation
     * occurred.
     */
    final Set<Effect> callFx = effects.getMethodCallEffects(bcaQuery, mcall, enclosingDecl); 
    for (final Effect effect : callFx) {
      if (effect.isEmpty()) {
        continue;
      } 
      final AggregationEvidence lastAgg = LastAggregationProcessor.get(effect);
      if (lastAgg != null) {
        getLocksFromEffect(effect, lastAgg, result);
      } else {
        final Target target = effect.getTarget();
        if (target instanceof ClassTarget || target instanceof AnyInstanceTarget) {
          for (final Target exposedTarget : exposedTargets) {
            if (conflicter.doTargetsOverlap(target, exposedTarget)) {
              getLocksForDirectRegionAccess(
                  effects, bcaQuery, rcvr, mcall, effect.isRead(), exposedTarget, result);
            }
          }
        }
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
  private void getLocksFromEffect(final Effect effect,
      final AggregationEvidence lastAgg, final Set<NeededLock> result) {
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
    if (lastAgg != null) { // We have aggregation
      final IRegion aggedRegion = lastAgg.getOriginalRegion();
      final Map<IRegion, IRegion> aggMap = lastAgg.getRegionMapping();
      for (final Map.Entry<IRegion, IRegion> mapping : aggMap.entrySet()) {
        if (aggedRegion.ancestorOf(mapping.getKey())) {
          final IRegion destRegion = mapping.getValue();
          if (destRegion.isStatic()) {
            targets.add(targetFactory.createClassTarget(destRegion, NoEvidence.INSTANCE));
          } else {
            final IRNode objExpr;
            if (target instanceof ClassTarget) {
              objExpr = FieldRef.getObject(lastAgg.getOriginalExpression());
            } else {
              objExpr = target.getReference();
            }
            /* The only way we can get here is if getLastAggregation() != null,
             * and this implies that that target has ElaborationEvidence
             */
            targets.add(targetFactory.createInstanceTarget(
                objExpr, destRegion, target.getEvidence()));
          }
        }
      }
    }
    
    final boolean isWrite = effect.isWrite();
    for (Target t : targets) {
      /* BCA only helps us if it yields a FieldRef expression that can then be
       * used with aggregation.  If aggregation doesn't occur after a BCA,
       * then we don't care about the result.  We take our given target and 
       * backtrack over BCA until we hit aggregation or the end.
       */
      t = UndoBCAProcessor.undo(t);
      
      final IRegion region = t.getRegion();
      /* Final regions do not need locks --- VOLATILE ones do! (changes this on 2012-03-30) */
      if (!region.isFinal()) {
        final IJavaType lookupRegionInThisType = t.getRelativeClass(binder);
        final RegionLockRecord neededLock =
          getLockForRegion(lookupRegionInThisType, region);
        if (neededLock != null) {
          final LockModel lm = neededLock.lockDecl;
          final ILock.Type type = ILock.Type.get(isWrite, lm.isReadWriteLock());
          if (t instanceof ClassTarget) {
            final NeededLock l =
              neededLockFactory.createStaticLock(lm, type);
            result.add(l);
          } else { // InstanceTarget
            final NeededLock l;
            if (neededLock.lockDecl.isLockStatic()) {
              l = neededLockFactory.createStaticLock(lm, type);
            } else {                
              l = neededLockFactory.createInstanceLock(
                  t.getReference(), lm, type);
            }
            result.add(l);
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
    /* No RequiresLock means no lock precondition. */
    if (reqLockD == null) {
      return locks;
    }
    final List<LockSpecificationNode> lockNames = reqLockD.getAAST().getLockList();
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

  public void convertLockExpr(final HowToProcessLocks howTo,
      final IRNode lockExpr, final HeldLockFactory heldLockFactory, final IRNode enclosingDecl, final IRNode src,
      final Set<HeldLock> lockSet) {
    convertLockExpr(howTo, lockExpr, heldLockFactory, enclosingDecl, Type.MONOTLITHIC, src, lockSet);
  }
  
  /**
   * Convert an expression known to refer to a lock
   * object into a set of declared locks.  The lock object can be an intrinsic
   * or java.util.concurrent lock.
   */
  public void convertIntrinsicLockExpr(
      final IRNode lockExpr, final HeldLockFactory heldLockFactory, final IRNode enclosingDecl, final IRNode src,
      final Set<HeldLock> lockSet) {
    /* We start by assuming we will be creating a monolithic lock object, and not a
     * read-write lock; thus, we set isWrite to true and isRW to false.
     */
    convertLockExpr(HowToProcessLocks.INTRINSIC, lockExpr, heldLockFactory, enclosingDecl, src, lockSet);
  }

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
      final IRNode lockExpr, final HeldLockFactory heldLockFactory, final IRNode enclosingDecl, final IRNode src, final Set<HeldLock> lockSet) {
    /* We start by assuming we will be creating a monolithic lock object
     */
    convertLockExpr(HowToProcessLocks.JUC, lockExpr, heldLockFactory, enclosingDecl, src, lockSet);
  }

  /**
   * Note: A lock expression always results in a non-{@link HeldLock#isAssumed() assumed} lock.
   * 
   */
  private void convertLockExpr(final HowToProcessLocks howTo,
      final IRNode lockExpr, final HeldLockFactory heldLockFactory, final IRNode enclosingDecl, 
      final ILock.Type type, final IRNode src, final Set<HeldLock> lockSet) {
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
        final ILock.Type newType = ILock.Type.getRW(whichMethod == ReadWriteLockMethods.WRITELOCK);
        final MethodCall mcall = (MethodCall) op;
        convertLockExpr(howTo, mcall.get_Object(lockExpr), heldLockFactory, enclosingDecl, newType, src, lockSet);
      } else {
        // Dealing with a normal lock
        final HeldLock returnedLock =
          convertReturnedLock(lockExpr, heldLockFactory, enclosingDecl, type, src);
        if (returnedLock != null) {
          lockSet.add(returnedLock);
        } else {
          // method doesn't return a known lock, so nothing to do.
        }
      }
    } else {
      if (howTo == HowToProcessLocks.INTRINSIC) {
        /* First see if the expression itself results in an object that uses
         * itself as a lock. ThisExpressions and VariableUseExpressions are
         * trivially handled here. We do not do this for method calls because the
         * returned object does not yet have a fixed name.
         * 
         * This only applies to intrinsic locks because "this" only refers to a
         * JUC lock if we are implementing the interface Lock or ReadWriteLock.
         * Therefore, we always generate a write-enabled non-readWrite lock.
         */
        for (AbstractLockRecord lr : sysLockModelHandle.get().getRegionAndPolicyLocksForSelf(lockExpr)) {
          lockSet.add(
              heldLockFactory.createInstanceLock(lockExpr, lr.lockDecl, src, null, false, Type.MONOTLITHIC));
        }
    
        /*
         * Now see if the expression is a FieldRef or ClassExpression (which for
         * our purposes is a special kind of FieldRef). If so, see if the field is
         * distinguished as a lock.
         * 
         * A class expression can never be a JUC lock because the reference type 
         * is always java.lang.Class.
         */
        if (ClassExpression.prototype.includes(op)) { // lockExpr == 'e.class'
          final IRNode cdecl = this.binder.getBinding(lockExpr); // get the class being locked
          // Check for state locks
          for (final AbstractLockRecord lr :
            sysLockModelHandle.get().getRegionAndPolicyLocksForLockImpl(
                JavaTypeFactory.getMyThisType(cdecl), cdecl)) {
            lockSet.add(
                heldLockFactory.createStaticLock(lr.lockDecl, src, null, false, Type.MONOTLITHIC));
          }
        }
      }
      
      if (FieldRef.prototype.includes(op)) { // lockExpr == 'e.f'
        final IRNode obj = FieldRef.getObject(lockExpr);
        final IJavaType objType = binder.getJavaType(obj);
        final IRNode potentialLockImpl = this.binder.getBinding(lockExpr);

        // see if 'f' is a lock in class typeOf(e)
        // reminder: lockExpr is a FieldRef; binding it gives the field decl
        final Set<AbstractLockRecord> records =
          sysLockModelHandle.get().getRegionAndPolicyLocksForLockImpl(
              objType, potentialLockImpl);
        if (TypeUtil.isStatic(potentialLockImpl)) {
          for (final AbstractLockRecord lr : records) {
            lockSet.add(heldLockFactory.createStaticLock(lr.lockDecl, src, null, false, type));
          }
        } else {
          for (final AbstractLockRecord lr : records) {
            // If we only have a fieldRef, then it must a Lock and not a ReadWriteLock, so it is write lock
            lockSet.add(heldLockFactory.createInstanceLock(obj, lr.lockDecl, src, null, false, type));
          }
        }
        
        if (howTo == HowToProcessLocks.JUC) {
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
                  final Type lockType = Type.getRW(whichMethod == ReadWriteLockMethods.WRITELOCK);
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
                                  objType, mcBoundField);
                            for (final AbstractLockRecord lr : records2) {
                              lockSet.add(
                                  heldLockFactory.createStaticLock(lr.lockDecl, src, null, false, lockType));
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
                                  frObjectType, mcBoundField);
                            for (final AbstractLockRecord lr : records2) {
                              /* NB. Here we use the 'obj', the receiver from
                               * the original FieldRef above! We are doing an
                               * alpha-renaming of the original receiver
                               * expression for 'this'.
                               */
                              lockSet.add(
                                  heldLockFactory.createInstanceLock(obj, lr.lockDecl, src, null, false, lockType));
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
   * Convert the AAST lock type flag to an analysis lock type flag.
   */
  private static ILock.Type convertType(final LockType original) {
    return (original == LockType.RAW) ? Type.MONOTLITHIC : 
      (original == LockType.WRITE_LOCK) ? Type.WRITE : Type.READ;
  }
  
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
      final IRNode mcall, final HeldLockFactory heldLockFactory, final IRNode callingDecl, final IRNode src) {
    return convertReturnedLock(mcall, heldLockFactory, callingDecl, Type.MONOTLITHIC, src);
  }

  // Converts returned lock for a caller of the method with the annotation
  // callingDecl is the method/constructor that contains mcall
  private HeldLock convertReturnedLock(
      final IRNode mcall, final HeldLockFactory heldLockFactory, final IRNode callingDecl, final ILock.Type type,
      final IRNode src) {
    // See if the method even returns a lock
    final IRNode mdecl                        = binder.getBinding(mcall);
    final ReturnsLockPromiseDrop returnedLock = LockUtils.getReturnedLock(mdecl);
    if (returnedLock != null) {
      final Map<IRNode, IRNode> m = MethodCallUtils.constructFormalToActualMap(
          binder, mcall, mdecl, callingDecl);
      return convertHeldLockNameToCallerContext(
          mdecl, heldLockFactory, returnedLock.getAAST().getLock(), type, src, m);
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
  public static HeldLock convertLockNameToMethodContext(
      final IRNode mdecl, final HeldLockFactory heldLockFactory,
      final LockSpecificationNode lockSpec, final boolean isAssumed,
      final RequiresLockPromiseDrop supportingDrop,
      final IRNode formalRcvr, final Collection<LockSpecificationNode> locksOnParameters) {
    final IRNode src = lockSpec.getPromisedFor();
    final LockModel lockDecl = lockSpec.resolveBinding().getModel();
    final LockNameNode lockName = lockSpec.getLock();
    final ILock.Type lockType = convertType(lockSpec.getType());
    
    if (lockDecl.isLockStatic()) {
      return heldLockFactory.createStaticLock(lockDecl, src, supportingDrop, isAssumed, lockType);
    } else {
      if (lockName instanceof SimpleLockNameNode) {
        // Lock is "this.<LockName>"
        return heldLockFactory.createInstanceLock(formalRcvr, lockDecl, src, supportingDrop, isAssumed, lockType);
      } else { // QualifiedLockNameNode
        final ExpressionNode base = ((QualifiedLockNameNode) lockName).getBase();
        if (base instanceof TypeExpressionNode) {
          // Should be handled in the "isLockStatic" case above!
          throw new IllegalStateException("Lock should be static, but isLockStatic() is false");
        } else if (base instanceof ThisExpressionNode) {
          // Lock is "this.<LockName>"
          return heldLockFactory.createInstanceLock(formalRcvr, lockDecl, src, supportingDrop, isAssumed, lockType);
        } else if (base instanceof QualifiedThisExpressionNode) {
          final QualifiedThisExpressionNode qthis = (QualifiedThisExpressionNode) base;
          final IRNode canonicalQThis =
            JavaPromise.getQualifiedReceiverNodeByName(
                mdecl, qthis.getType().resolveType().getNode());
          if (canonicalQThis == null) {
        	  return null;
          }
          // Lock is "x.y.Z.this.<LockName>"
          return heldLockFactory.createInstanceLock(canonicalQThis, lockDecl, src, supportingDrop, isAssumed, lockType);
        } else {
          // Lock is "<UseExpression>.<LockName>"
          if (locksOnParameters != null) locksOnParameters.add(lockSpec);
          return heldLockFactory.createInstanceLock(
              base, lockDecl, src, supportingDrop, isAssumed, lockType);
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
    final Type type = convertType(lockSpec.getType());
    if (lockModel.isLockStatic()) {
      return neededLockFactory.createStaticLock(lockModel, type);
    } else {
      final IRNode objExpr = 
        convertObjectExpressionToCallerContext(mdecl, lockName, map);
      if (objExpr != null) {
        return neededLockFactory.createInstanceLock(
            objExpr, lockModel, type);
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
  public static HeldLock convertHeldLockNameToCallerContext(
      final IRNode mdecl, final HeldLockFactory heldLockFactory, final LockNameNode lockName, final ILock.Type type,
      final IRNode src, final Map<IRNode, IRNode> map) {
    final LockModel lockModel = lockName.resolveBinding().getModel();  
    if (lockModel.isLockStatic()) {
      return heldLockFactory.createStaticLock(lockModel, src, null, false, type);
    } else {
      final IRNode objExpr =
        convertObjectExpressionToCallerContext(mdecl, lockName, map);
      return heldLockFactory.createInstanceLock(objExpr, lockModel, src, null, false, type);
    }
  }

  private static IRNode convertObjectExpressionToCallerContext(
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
  
  public LockModel getMutex() {
    return mutex;
  }
  
  public InstanceTarget createInstanceTarget(
      final IRNode object, final IRegion region) {
    return targetFactory.createInstanceTarget(object, region, NoEvidence.INSTANCE);
  }

  public ClassTarget createClassTarget(final IRegion field) {
    return targetFactory.createClassTarget(field, NoEvidence.INSTANCE);
  }

  /**
   * Determine if a constructor can be considered to be single-threaded
   * 
   * @param cdecl
   *          The constructor declaration node of the constructor to be tested.
   * @param rcvrDecl
   *          The receiver declaration node associated with the constructor
   *          declaration.
   */
  /* Could move this to LockExpressions, but I like it better here because
   * LockUtils contains all the methods for complex operations involving lock
   * semantics.
   */
  public LockExpressions.SingleThreadedData isConstructorSingleThreaded(
      final IRNode cdecl, final IRNode rcvrDecl) {
    // get the receiver and see if it is declared to be borrowed
    final BorrowedPromiseDrop bDrop = UniquenessRules.getBorrowed(rcvrDecl);
    final boolean isBorrowedThis = bDrop != null;
    
    // See if the return value is declared to be unique
    final IRNode returnNode = JavaPromise.getReturnNodeOrNull(cdecl);
    final UniquePromiseDrop uDrop = UniquenessRules.getUnique(returnNode);
    final boolean isUniqueReturn = uDrop != null;

    /*
     * See if the declared *write* effects are < "writes this.Instance" (Can
     * read whatever it wants. Want to prevent the object from writing a
     * reference to itself into another object.
     */
    /*
     * We can use the default target factory here because we are passing it the
     * receiver declaration node directly.
     */
    final Effect writesInstance = Effect.newWrite(null,
        DefaultTargetFactory.PROTOTYPE.createInstanceTarget(
            rcvrDecl, RegionModel.getInstanceRegion(cdecl), NoEvidence.INSTANCE));

    final List<Effect> declFx = Effects.getDeclaredMethodEffects(cdecl, cdecl);
    /* Ultimately this is only used if the effects are declared and of
     * interest.
     */
    final RegionEffectsPromiseDrop eDrop = MethodEffectsRules.getRegionEffectsDrop(cdecl);
    final StartsPromiseDrop teDrop = ThreadEffectsRules.getStartsSpec(cdecl);
    boolean isEffectsWork = teDrop != null;
    if (isEffectsWork && declFx != null) {
      final Iterator<Effect> iter = declFx.iterator();
      while (isEffectsWork && iter.hasNext()) {
        final Effect effect = iter.next();
        if (effect.isWrite()) {
          isEffectsWork &= effect.isCheckedBy(binder, writesInstance);
        }
      }
    }
    return new LockExpressions.SingleThreadedData(isBorrowedThis, bDrop,
        isUniqueReturn, uDrop, isEffectsWork, eDrop, teDrop);
  }
  
  /**
   * Given a synchronized method, return the locks it acquires. 
   * 
   * @param mdecl
   *          The declaration node for the synchronized method. This method does
   *          <em>not</em> test whether the method is declared to be
   *          synchronized.  This method assumes it is being called from a context
   *          in which the body of mdecl is being analyzed, and thus the 
   *          field {@link #ctxtTheReceiverNode} refers to the canonical receiver
   *          for this method.
   * @param cdecl
   *          The class declaration node for the class in which it is declared.
   * @param lockStack
   *          A linked list of locks that that is modified as a result of this
   *          method. The locks corresponding to the synchronization are added
   *          to the front of the list.
   */
  public void convertSynchronizedMethod(
      final IRNode mdecl, final HeldLockFactory heldLockFactory, final IRNode rcvr, final IJavaDeclaredType clazz,
      final IRNode cdecl, final Set<HeldLock> result) {
    // is the method static?
    if (TypeUtil.isStatic(mdecl)) {
      // Look up the class definition (which is used to represent the class
      // lock)
      final Set<AbstractLockRecord> records =
        sysLockModelHandle.get().getRegionAndPolicyLocksForLockImpl(clazz, cdecl);
      for (final AbstractLockRecord lr : records) {
        // Synchronized methods use intrinsic locks, so they are always write locks
        result.add(
            heldLockFactory.createStaticLock(lr.lockDecl, mdecl, null, false, Type.MONOTLITHIC));
      }
    } else {
      // is the receiver a known lock?
      final Set<AbstractLockRecord> records =
        sysLockModelHandle.get().getRegionAndPolicyLocksForLockImpl(clazz, GlobalLockModel.THIS);
      for (final AbstractLockRecord lr : records) {
        // Synchronized methods use intrinsic locks, so they are always write locks
        result.add(
            heldLockFactory.createInstanceLock(rcvr, lr.lockDecl, mdecl, null, false, Type.MONOTLITHIC));
      }
    }
  }

  public static void getLockPreconditions(
      final HowToProcessLocks howTo, final IRNode methodDecl,
      final HeldLockFactory heldLockFactory, final IRNode rcvr,
      final Set<HeldLock> preconditions,
      final Collection<LockSpecificationNode> locksOnParameters) {
    final RequiresLockPromiseDrop drop = LockRules.getRequiresLock(methodDecl);
    /* No RequiresLock means no lock precondition. */
    if (drop != null) {
      for(final LockSpecificationNode requiredLock : drop.getAAST().getLockList()) {
        final LockModel lm = requiredLock.resolveBinding().getModel();
        if (howTo.acceptsLock(lm)) {
          final HeldLock lock = convertLockNameToMethodContext(
              methodDecl, heldLockFactory, requiredLock, true, drop, rcvr, 
              locksOnParameters);
          preconditions.add(lock);
        }
      }
    }
  }
  
  public void getClassInitLocks(
      final HowToProcessLocks howTo, final IRNode classInitDecl,
      final HeldLockFactory heldLockFactory,
      final IJavaDeclaredType classBeingInitialized, final Set<HeldLock> assumedLocks) {
    /* Go through all the STATE locks in the class and pick out all the
     * locks that protect static regions. 
     */
    final Set<RegionLockRecord> records =
      sysLockModelHandle.get().getRegionLocksInClass(classBeingInitialized);
    for (final RegionLockRecord lr : records) {
      if (lr.region.isStatic() && howTo.acceptsLock(lr.lockDecl)) {
        final Type lockType = howTo.getAssumedLockType(lr.lockDecl);
        final HeldLock lock = heldLockFactory.createStaticLock(lr.lockDecl, classInitDecl, null, false, lockType);
        assumedLocks.add(lock);
      }
    }
  }
  
  public void getSingleThreadedLocks(
      final HowToProcessLocks howTo, final IRNode conDecl,
      final HeldLockFactory heldLockFactory,
      final IJavaDeclaredType clazz, final IRNode rcvr,
      final Set<HeldLock> assumedLocks) {
    /*
     * Go through all the STATE locks in the class and pick out all the locks that
     * protect instance regions. Caveat: We exclude the lock MUTEX for Object
     * because we do not want to be able to verify wait() and notify() calls as
     * result of the @synchronized annotation.
     */
    final Set<RegionLockRecord> records = sysLockModelHandle.get().getRegionLocksInClass(clazz);
    for (final RegionLockRecord lr : records) {
      if (howTo.acceptsLock(lr.lockDecl) && !lr.region.isStatic() && (lr.lockDecl != mutex)) {
        final Type lockType = howTo.getAssumedLockType(lr.lockDecl);
        final HeldLock lock =
          heldLockFactory.createInstanceLock(rcvr, lr.lockDecl, conDecl, null, false, lockType);
        assumedLocks.add(lock);
      }
    }
  }
}
