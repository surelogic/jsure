package com.surelogic.analysis.concurrency.heldlocks_new;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableSet;
import com.surelogic.analysis.MethodCallUtils;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.assigned.DefiniteAssignment.ProvablyUnassignedQuery;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.concurrency.model.AnalysisLockModel;
import com.surelogic.analysis.concurrency.model.declared.ModelLock;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLock;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLockFactory;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLock.Reason;
import com.surelogic.analysis.effects.ConflictChecker;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.effects.NoEffectEvidence;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.evidence.NoEvidence;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.locks.ReturnsLockPromiseDrop;

import edu.cmu.cs.fluid.CommonStrings;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaPromise;
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
import edu.cmu.cs.fluid.tree.Operator;


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
  /** The name of the interface {@code java.util.concurrent.locks.Lock}. */
  public static final String JAVA_UTIL_CONCURRENT_LOCKS_LOCK = "java.util.concurrent.locks.Lock";

  /** The name of the interface {@code java.util.concurrent.locks.ReadWriteLock}. */
  public static final String JAVA_UTIL_CONCURRENT_LOCKS_READWRITELOCK = "java.util.concurrent.locks.ReadWriteLock";

  /**
   * The name of the {@code unlock} method of
   * {@code java.util.concurrent.locks.Lock}.
   */
  public static final String UNLOCK = CommonStrings.intern("unlock");

  /**
   * The name of the {@code lock} method of
   * {@code java.util.concurrent.locks.Lock}.
   */
  public static final String LOCK = CommonStrings.intern("lock");

  /**
   * The name of the {@code lock} method of
   * {@code java.util.concurrent.locks.Lock}.
   */
  public static final String TRYLOCK = CommonStrings.intern("tryLock");

  /**
   * The name of the {@code lockInterruptibly} method of
   * {@code java.util.concurrent.locks.Lock}.
   */
  public static final String LOCKINTERRUPTIBLY = CommonStrings.intern("lockInterruptibly"); 
  
  /**
   * The name of other {@code readLock} method of
   * {code java.util.concurrent.locks.ReadWriteLock}.
   */
  public static final String READLOCK = CommonStrings.intern("readLock");

  /**
   * The name of other {@code writeLock} method of
   * {code java.util.concurrent.locks.ReadWriteLock}.
   */
  public static final String WRITELOCK = CommonStrings.intern("writeLock");

  
  
  /** Handle to the lock model. */
  private final AtomicReference<AnalysisLockModel> analysisLockModel;
  /**
   * The binder to use.
   */
  private final ThisExpressionBinder thisExprBinder;
  
  /** The effects analysis to use; for final expression checking. */
  private final Effects effects;
  
  /** Effects conflict checker for use by final expression checking. */
  private final ConflictChecker conflictChecker;
  
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

  
  
  // ========================================================================
  // == Constructor
  // ========================================================================

  /**
   * Create a set of lock utility methods based around a given set of 
   * analysis information.
   * @param glm The global lock model to use.
   * @param ea The effects analysis to use.
   */
  public LockUtils(final AtomicReference<AnalysisLockModel> lm,
      final ThisExpressionBinder teb,
      final Effects effects, final IMayAlias mayAlias) {
    this.analysisLockModel = lm;
    this.thisExprBinder = teb;
    this.effects = effects;
    this.conflictChecker = new ConflictChecker(teb, mayAlias);

    if (thisExprBinder == null || thisExprBinder.getTypeEnvironment() == null) {
      throw new IllegalStateException();
    }
    lockType = JavaTypeFactory.convertNodeTypeToIJavaType(
        thisExprBinder.getTypeEnvironment().findNamedType(
            JAVA_UTIL_CONCURRENT_LOCKS_LOCK), thisExprBinder);
    readWriteLockType = JavaTypeFactory.convertNodeTypeToIJavaType(
        thisExprBinder.getTypeEnvironment().findNamedType(
            JAVA_UTIL_CONCURRENT_LOCKS_READWRITELOCK), thisExprBinder);
  }
  
  
  
  // ========================================================================
  // == Tests
  // ========================================================================

  /**
   * Is the method declared to return a lock?
   */
  public boolean isLockGetterMethod(final IRNode mcall) {
    final IRNode mdecl = thisExprBinder.getBinding(mcall);
    final IRNode returnNode = JavaPromise.getReturnNodeOrNull(mdecl);
    if (returnNode == null) {
      return false;
    } else {
      return LockRules.getReturnsLock(returnNode) != null;
    }
  }
  
  
  
  // ========================================================================
  // == Test for use of java.util.concurrent
  // ========================================================================

  private boolean isMethodFrom(final IRNode mcall, final IJavaType testType) {
	  IBinding b = thisExprBinder.getIBinding(mcall);
	  if (b == null) {
		  SLLogger.getLogger().warning("No binding for "+DebugUnparser.toString(mcall));
		  return false;
	  }
	  IJavaType context = b.getContextType();
	  if (context == null) {
		  IRNode tdecl = VisitUtil.getEnclosingType(b.getNode());
		  context = thisExprBinder.getTypeEnvironment().convertNodeTypeToIJavaType(tdecl);
	  }
	  return thisExprBinder.getTypeEnvironment().isRawSubType(context, testType);
  }
  
  public enum LockMethods {
    LOCK,
    LOCKINTERRUPTIBLY,
    TRYLOCK,
    UNLOCK,
    NOT_A_LOCK_METHOD,
    IDENTICALLY_NAMED_METHOD; 
    
    public static LockMethods whichLockMethod(final String mname) {
      final String internedName = CommonStrings.intern(mname);
      if (internedName == LockUtils.LOCK) {
        return LockMethods.LOCK;
      } else if (internedName == LockUtils.LOCKINTERRUPTIBLY) {
        return LockMethods.LOCKINTERRUPTIBLY;
      } else if (internedName == LockUtils.TRYLOCK) {
        return LockMethods.TRYLOCK;
      } else if (internedName == LockUtils.UNLOCK) {
        return LockMethods.UNLOCK;
      } else {
        return LockMethods.NOT_A_LOCK_METHOD;
      }
    }
  }

  /**
   * Test if the given method call node calls a method declared in 
   * interface {@code java.util.concurrent.locks.Lock}.
   */
  public boolean isMethodFromJavaUtilConcurrentLocksLock(final IRNode mcall) {
    final String internedName = CommonStrings.intern(MethodCall.getMethod(mcall));
    return (internedName == LOCK || internedName == UNLOCK || internedName == LOCKINTERRUPTIBLY) &&
        isMethodFrom(mcall, lockType);
  }
  
  public LockMethods whichLockMethod(final IRNode mcall) {
    final LockMethods which = LockMethods.whichLockMethod(MethodCall.getMethod(mcall));
    if (isMethodFrom(mcall, lockType)) {
      return which;
    } else {
      return which == LockMethods.NOT_A_LOCK_METHOD ? which : LockMethods.IDENTICALLY_NAMED_METHOD; 
    }
  }
  
  public enum ReadWriteLockMethods {
    READLOCK,
    WRITELOCK,
    NOT_A_READWRITELOCK_METHOD;
    
    public static ReadWriteLockMethods whichReadWriteLockMethod(final String mname) {
      final String internedName = CommonStrings.intern(mname);
      if (internedName == LockUtils.READLOCK) {
        return ReadWriteLockMethods.READLOCK;
      } else if (internedName == LockUtils.WRITELOCK) {
        return ReadWriteLockMethods.WRITELOCK;
      } else {
        return ReadWriteLockMethods.NOT_A_READWRITELOCK_METHOD;
      }
    }
  }
  
  /**
   * Test if the given method call node calls a method declared in 
   * interface {@code java.util.concurrent.locks.Lock}.
   */
  public boolean isMethodFromJavaUtilConcurrentLocksReadWriteLock(final IRNode mcall) {
    final String internedName = CommonStrings.intern(MethodCall.getMethod(mcall));
    return (internedName == READLOCK || internedName == WRITELOCK) &&
        isMethodFrom(mcall, readWriteLockType);
  }
  
  public ReadWriteLockMethods whichReadWriteLockMethod(final IRNode mcall) {
    if (isMethodFrom(mcall, readWriteLockType)) {
      return ReadWriteLockMethods.whichReadWriteLockMethod(MethodCall.getMethod(mcall));
    } else {
      return ReadWriteLockMethods.NOT_A_READWRITELOCK_METHOD;
    }
  }
//  /**
//   * Return which method from {@code java.util.concurrent.locks.Lock} is
//   * being called.
//   */
//  public LockMethods whichLockMethod(final IRNode mcall) {
//    final LockMethods method = LockMethods.whichLockMethod(MethodCall.getMethod(mcall));
//    if (isMethodFromJavaUtilConcurrentLocksLock(mcall)) {
//      return method;
//    } else {
//      return method == LockMethods.NOT_A_LOCK_METHOD ? method : LockMethods.IDENTICALLY_NAMED_METHOD;
//    }
//  }
  
//  /**
//   * Return which method from {@code java.util.concurrent.locks.Lock} is
//   * being called.
//   */
//  public ReadWriteLockMethods whichReadWriteLockMethod(final IRNode mcall) {
//    final ReadWriteLockMethods method = ReadWriteLockMethods.whichReadWriteLockMethod(MethodCall.getMethod(mcall));
//    if (isMethodFromJavaUtilConcurrentLocksReadWriteLock(mcall)) {
//      return method;
//    } else {
//      return method == ReadWriteLockMethods.NOT_A_READWRITELOCK_METHOD ? method : ReadWriteLockMethods.IDENTICALLY_NAMED_METHOD;
//    }
//  }
  
//  /**
//   * Test if a class implements {@code java.util.concurrent.locks.Lock}.
//   * 
//   * @param type
//   *          The java type to test
//   */
//  public boolean implementsLock(final IJavaType type) {
//  	if (lockType == null) {
//  	    // Probably running on pre-1.5 code
//  		return false;
//  	}
//    if (type instanceof IJavaDeclaredType) {
//      return thisExprBinder.getTypeEnvironment().isRawSubType(type, lockType);
//    } else {
//      // Arrays and primitives are not lock types
//      return false;
//    }
//  }
//
//  /**
//   * Test if a class implements {@code java.util.concurrent.locks.ReadWriteLock}.
//   * 
//   * @param type
//   *          The java type to test
//   */
//  public boolean implementsReadWriteLock(final IJavaType type) {
//  	if (readWriteLockType == null) {
//  	    // Probably running on pre-1.5 code
//  		return false;
//  	}
//    if (type instanceof IJavaDeclaredType) {
//      return thisExprBinder.getTypeEnvironment().isRawSubType(type, readWriteLockType);
//    } else {
//      // Arrays and primitives are not lock types
//      return false;
//    }
//  }

  
  
  // ========================================================================
  // == Test for final expressions
  // ========================================================================

  /**
   * Check if an expression is "final": if its value is fixed no matter where 
   * in the flow of control it is evaluated.
   * 
   * @param syncBlock
   *          The synchronized block, if any, whose lock expression contains
   *          <code>expr</code>. May be <code>null</code> if the expression is
   *          not part of the lock expression of a synchronized block.
   */
  public final boolean isFinalExpression(
      final IRNode exprToTest, final IRNode flowUnit, final IRNode syncBlock,
      final BindingContextAnalysis.Query bcaQuery,
      final ProvablyUnassignedQuery unassignedQuery) {
    /*
     * TODO Need to modify this method to produce
     * a nested chain of evidence of why it believes an expression to NOT be a
     * final expression. Chain should contain links to the field/var declaration
     * showing that the variable is not declared to be final, etc.
     */
    final Effects.Query fxQuery = effects.getEffectsQuery(flowUnit, bcaQuery);

    /* Use an inner class to that we can recurse but regenerate the fxQuery 
     * every time.
     */
    abstract class Context {
      public abstract boolean isFinal(final IRNode exprToTest);
    }
    
    final Context context = new Context() {
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
          final IRNode expr, final IRNode sync) {
        if (sync != null) {
          final Set<Effect> bodyEffects = fxQuery.getResultFor(sync);
          final Set<Effect> exprEffects = fxQuery.getResultFor(expr);
          return conflictChecker.mayConflict(bodyEffects, exprEffects);
        } else {
          return true;
        }
      }

      private boolean isArrayChangedBySyncBlock(
          final IRNode array, final IRNode sync) {
        if (sync != null) {
          final Set<Effect> exprEffects = Collections.singleton(
              Effect.read(null,
                  new InstanceTarget(thisExprBinder.bindThisExpression(array),
                      RegionModel.getInstanceRegion(sync), NoEvidence.INSTANCE),
                  NoEffectEvidence.INSTANCE));
          final Set<Effect> bodyEffects = fxQuery.getResultFor(sync);
          return conflictChecker.mayConflict(bodyEffects, exprEffects);
        } else {
          return true;
        }
      }
      
      @Override
      public boolean isFinal(final IRNode exprToTest) {
        final Operator op = JJNode.tree.getOperator(exprToTest);
        if (CastExpression.prototype.includes(op)) {
          // Final if the nested expression is final
          return isFinal(CastExpression.getExpr(exprToTest));
        } else if (ParenExpression.prototype.includes(op)) {
          // Final if the nested expression is final
          return isFinal(ParenExpression.getOp(exprToTest));
        } else if (MethodCall.prototype.includes(op)) {
          MethodCall mcall = (MethodCall) op;
          /* Object expression must be final or method must be static, and the method
           * must have a @returnsLock annotation (which we are using as a very
           * specific idempotency annotation) or a call to readLock() or writeLock().
           */
          final IRNode mdecl = thisExprBinder.getBinding(exprToTest);
          if (TypeUtil.isStatic(mdecl)
              || isFinal(mcall.get_Object(exprToTest))) {
            return (LockRules.getReturnedLock(mdecl) != null) ||
                isMethodFromJavaUtilConcurrentLocksReadWriteLock(exprToTest);
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
          final IRNode id = thisExprBinder.getBinding(exprToTest);
          if (TypeUtil.isFinalOrEffectivelyFinal(id, thisExprBinder, unassignedQuery)) {
            return true;
          } else {
            return !isLockExpressionChangedBySyncBlock(exprToTest, syncBlock);
          }
        } else if (FieldRef.prototype.includes(op)) {
          /* Field must be final (or protected by a lock and not modified by the
           * body of the synchronized block) AND either the field must be static or
           * the object expression must be final
           */
          final IRNode id = thisExprBinder.getBinding(exprToTest);

          /* Check that the object expression is final (or static) */
          if (TypeUtil.isStatic(id)
              || isFinal(FieldRef.getObject(exprToTest))) {
            // Check if the field is final
            if (TypeUtil.isJSureFinal(id)) {
              return true;
            } else {
              /* Check if the field is protected by a lock and is not modified by
               * the body of the synchronized block.
               */
              return analysisLockModel.get().getLockForFieldRef(exprToTest) != null
                  && !isLockExpressionChangedBySyncBlock(exprToTest, syncBlock);
            }
          }
        } else if (ArrayRefExpression.prototype.includes(op)) {
          /* Array ref expression e[e']. Expressions e and e' must be final
           * expressions. The effects of the synchronized block must not conflict
           * with reading from the array.
           */
          final IRNode array = ArrayRefExpression.getArray(exprToTest);
          final IRNode idx = ArrayRefExpression.getIndex(exprToTest);
          if (isFinal(array) && isFinal(idx)) {
            return !isArrayChangedBySyncBlock(array, syncBlock);
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
    
    return context.isFinal(exprToTest);
  }
  
  
  
  // ========================================================================
  // == Convert expressions of various types to locks
  // ========================================================================
  
  private class LockExpressionConverter {
    // If false, convert as JUC
    private final boolean convertAsIntrinsic;
    private final IRNode src;
    private final Reason reason;
    private final HeldLockFactory heldLockFactory;
    private final ProvablyUnassignedQuery query;
    private final IRNode enclosingDecl;
    private final ImmutableSet.Builder<HeldLock> locks;
    
    public LockExpressionConverter(
        final boolean convertAsIntrinsic, final IRNode src, final Reason reason,
        final HeldLockFactory heldLockFactory, final ProvablyUnassignedQuery query,
        final IRNode enclosingDecl, final ImmutableSet.Builder<HeldLock> locks) {
      this.convertAsIntrinsic = convertAsIntrinsic;
      this.src = src;
      this.reason = reason;
      this.heldLockFactory = heldLockFactory;
      this.query = query;
      this.enclosingDecl = enclosingDecl;
      this.locks = locks;
    }
    
    private void addInstanceLock(final IRNode objExpr,
        final ModelLock<?, ?> modelLock, final boolean isWrite) {
      locks.add(heldLockFactory.createInstanceLock(
          objExpr, modelLock, src, reason, isWrite, null));
    }
    
    private void addStaticLock(
        final ModelLock<?, ?> modelLock, final boolean isWrite) {
      locks.add(heldLockFactory.createStaticLock(
          modelLock, src, reason, isWrite, null));
    }
    
    private HeldLock convertReturnedLock(
        final IRNode methodCall, final boolean isWrite) {
      final IRNode methodDecl = thisExprBinder.getBinding(methodCall);
      final IRNode returnNode = JavaPromise.getReturnNodeOrNull(methodDecl);
      final ReturnsLockPromiseDrop returnsLock = LockRules.getReturnedLock(returnNode);
      if (returnsLock != null) {
        final Map<IRNode, IRNode> m = MethodCallUtils.constructFormalToActualMap(
            thisExprBinder, methodCall, methodDecl, enclosingDecl);
        return analysisLockModel.get().getHeldLockFromReturnsLock(
            returnsLock, methodDecl, isWrite, src, reason, m, heldLockFactory);
      } else {
        return null;
      }
    }
    
    // Returns true if the expression was successfully converted to a lock
    private boolean convertMethodCallToLock(
        final IRNode methodCall, final boolean createWrite) {
      final ReadWriteLockMethods whichMethod = whichReadWriteLockMethod(methodCall);
      if (whichMethod != ReadWriteLockMethods.NOT_A_READWRITELOCK_METHOD) {
        /* Dealing with a read-write lock: We update the "createWrite" argument
         * in the recursive call based on whether readLock() or writeLock()
         * is being called.
         */
        final MethodCall mcall = (MethodCall) JJNode.tree.getOperator(methodCall);
        convertLockExpr(mcall.get_Object(methodCall), whichMethod == ReadWriteLockMethods.WRITELOCK);
        return true;
      } else {
        // Try to see if we have a lock-getter method
        final HeldLock returnedLock =
          convertReturnedLock(methodCall, createWrite);
        if (returnedLock != null) {
          locks.add(returnedLock);
          return true;
        } else {
          // method doesn't return a known lock, so nothing to do.
          return false;
        }
      }
    }
    
    private boolean convertVariableUseToLock(
        final IRNode lockExpr, final boolean createWrite) {
      /* See if the variable use is of a final variable that was initialized to
       * a read/write lock component or to the result of a lock getter method:
       * 
       *   final Object o = this.lock;
       *   final Lock lock = rwLock.readLock();
       */
      final IRNode varDecl = thisExprBinder.getBinding(lockExpr);
      if (VariableDeclarator.prototype.includes(varDecl) &&
          TypeUtil.isFinalOrEffectivelyFinal(varDecl, thisExprBinder, query)) {
        // n.b. Parameters don't have inits, so we don't have to make a special test for them
        final IRNode init = VariableDeclarator.getInit(varDecl);
        if (Initialization.prototype.includes(init)) { // a real, non-empty init
          final IRNode initExpr = Initialization.getValue(init);
          if (MethodCall.prototype.includes(initExpr)) {
            /* We definitely have the use of local variable 'f',
             * where f is initialized to the result of a method call.
             */
            return convertMethodCallToLock(initExpr, createWrite);
          }
        }
      }
      return false;
    }
    
    private void convertSelf(final IRNode lockExpr) {
      /* This only applies to intrinsic locks because "this" only refers to a
       * JUC lock if we are implementing the interface Lock or ReadWriteLock.
       * Therefore, we always generate a write-enabled non-readWrite lock.
       */
      for (final ModelLock<?, ?> lock : analysisLockModel.get().getLocksImplementedByThis(
          thisExprBinder.getJavaType(lockExpr))) {
        addInstanceLock(lockExpr, lock, true);
      }
    }
    
    private void convertClassExpression(final IRNode lockExpr) {
      /* A class expression can never be a JUC lock because the reference type 
       * is always java.lang.Class.
       */
      for (final ModelLock<?, ?> lock : analysisLockModel.get().getLocksImplemetedByClass(
          thisExprBinder.getBinding(lockExpr))) {
        addStaticLock(lock, true);
      }
    }
    
    private void convertFieldRef(final IRNode fieldRef, final boolean isWrite) {
      final IRNode obj = FieldRef.getObject(fieldRef);
      final IJavaType objType = thisExprBinder.getJavaType(obj);
      final IRNode fieldDecl = thisExprBinder.getBinding(fieldRef);

      // fieldRef is "e.f" see if 'f' is a lock in class typeOf(e)
      final Iterable<ModelLock<?, ?>> modelLocks =
          analysisLockModel.get().getLocksImplementedByField(objType, fieldDecl);
      if (TypeUtil.isStatic(fieldDecl)) {
        for (final ModelLock<?, ?> lock: modelLocks) {
          addStaticLock(lock, isWrite);
        }
      } else {
        for (final ModelLock<?, ?> lock: modelLocks) {
          addInstanceLock(obj, lock, isWrite);
        }
      }
      
      if (!convertAsIntrinsic) {
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
        if (TypeUtil.isJSureFinal(fieldDecl)) { // Final field, check initialization
          final IRNode init = VariableDeclarator.getInit(fieldDecl);
          if (Initialization.prototype.includes(init)) {
            final IRNode initValue = Initialization.getValue(init);
            final Operator initValueOp = JJNode.tree.getOperator(initValue);
            if (MethodCall.prototype.includes(initValueOp)) {
              final ReadWriteLockMethods whichMethod = whichReadWriteLockMethod(initValue);
              if (whichMethod == ReadWriteLockMethods.READLOCK ||
                  whichMethod == ReadWriteLockMethods.WRITELOCK) {
                /* We have 'f = e.readLock()' or 'f = e.writeLock()' */
                final IRNode mcObject = ((MethodCall) initValueOp).get_Object(initValue);
                final Operator mcObjectOp = JJNode.tree.getOperator(mcObject);
                if (FieldRef.prototype.includes(mcObjectOp)) {
                  // We have 'f = e.g.readLock()' or 'f = e.g.writeLock()'
                  final IRNode mcBoundField = thisExprBinder.getBinding(mcObject);
                  // Field 'g' must be final
                  if (TypeUtil.isJSureFinal(mcBoundField)) {
                    if (TypeUtil.isStatic(mcBoundField)) {
                      // Check that 'f' and 'g' are both declared in the same class
                      if (TypeUtil.isStatic(fieldDecl)) {
                        final IRNode fClassBody = VisitUtil.getClosestType(fieldDecl);
                        final IRNode gClassBody = VisitUtil.getClosestType(mcBoundField);
                        if (fClassBody.equals(gClassBody)) {
                          for (final ModelLock<?, ?> lock : 
                            analysisLockModel.get().getLocksImplementedByField(
                                objType, mcBoundField)) {
                            addStaticLock(lock, whichMethod == ReadWriteLockMethods.WRITELOCK);
                          }
                        }
                      }
                    } else {
                      // Check that the FieldRef is "this.g"
                      final IRNode frObject = FieldRef.getObject(mcObject);
                      final Operator frObjectOp = JJNode.tree.getOperator(frObject);
                      if (ThisExpression.prototype.includes(frObjectOp)) {
                        // We have 'f = this.g.readLock()' or 'f = this.g.writeLock'
                        final IJavaType frObjectType = thisExprBinder.getJavaType(frObject);
                        if (frObjectType instanceof IJavaDeclaredType) { // sanity check
                          // see if 'g' is a lock in class typeOf(this)
                          for (final ModelLock<?, ?> lock : 
                            analysisLockModel.get().getLocksImplementedByField(
                                frObjectType, mcBoundField)) {
                            addInstanceLock(obj, lock, whichMethod == ReadWriteLockMethods.WRITELOCK);
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
    
    private void convertLockExpr(final IRNode lockExpr, final boolean isWrite) {
      final Operator op = JJNode.tree.getOperator(lockExpr);
      boolean handled = false;
      /* Check for special case of locally cached lock references first.  If this
       * doesn't find a lock, the expression is rechecked again below.
       */
      if (VariableUseExpression.prototype.includes(lockExpr)) {
        handled = convertVariableUseToLock(lockExpr, isWrite);
      }
      
      if (MethodCall.prototype.includes(op)) { /* For method calls that return the declared lock, if any */
        convertMethodCallToLock(lockExpr, isWrite);
      } else {
        /* If the expression was "handled" above as a variable use expression
         * we don't do this because we already got the lock by chasing the 
         * variable assignment.  So we don't want to consider what locks the 
         * lock itself represents.   That would be weird.
         */
        if (!handled && convertAsIntrinsic) {
          convertSelf(lockExpr);
          if (ClassExpression.prototype.includes(op)) { // lockExpr == 'e.class'
            convertClassExpression(lockExpr);
          }
        }
        if (FieldRef.prototype.includes(op)) { // lockExpr == 'e.f'
          convertFieldRef(lockExpr, isWrite);
        }
      }
    }
  }

  public void convertLockExpr(
      final boolean convertAsIntrinsic, final IRNode lockExpr,
      final HeldLockFactory heldLockFactory, final IRNode src,
      final Reason reason,
      final ProvablyUnassignedQuery query, final IRNode enclosingDecl, 
      final ImmutableSet.Builder<HeldLock> locks) {
    final LockExpressionConverter converter = new LockExpressionConverter(
        convertAsIntrinsic, src, reason, heldLockFactory, query, enclosingDecl, locks);
    converter.convertLockExpr(lockExpr, true);
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
}
