/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/MustReleaseAnalysis.java,v 1.34 2008/08/20 15:40:34 chance Exp $*/
package com.surelogic.analysis.locks;

//import java.io.IOException;
import java.text.MessageFormat;
import java.util.Set;
//import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.common.logging.SLLogger;
//import java.util.logging.Logger;

//import com.surelogic.logging.FluidLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.ReturnsLockPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.uwm.cs.fluid.control.BackwardAnalysis;
import edu.uwm.cs.fluid.control.FlowAnalysis;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis;

public final class MustReleaseAnalysis extends
    edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis<ImmutableList<ImmutableSet<IRNode>>[]> {
  private static final Logger LOGGER =
    SLLogger.getLogger("com.surelogic.analysis.locks.MustReleaseAnalysis");
      
//  static {
//    try {
//      final FileHandler handler = new FileHandler("%h/trace%u.log");
//      LOGGER.addHandler(handler);
//      LOGGER.setLevel(Level.ALL);
//      handler.setLevel(Level.ALL);
//      handler.setFormatter(new FluidLogger());
//    } catch (final IOException e) {
//      // swallow
//    }
//  }
  
  private final ThisExpressionBinder thisExprBinder; 
  private final LockUtils lockUtils;
  private final JUCLockUsageManager jucLockUsageManager;
  private final SimpleNonnullAnalysis nonNullAnalysis;
  
  
  
  public MustReleaseAnalysis(final ThisExpressionBinder teb, final IBinder b, final LockUtils lu,
      final JUCLockUsageManager lockMgr, final SimpleNonnullAnalysis sna) {
    super(b);
    thisExprBinder = teb;
    lockUtils = lu;
    jucLockUsageManager = lockMgr;
    nonNullAnalysis = sna;
  }

  
  
  // For debugging
  private static void log(
      final Level level, final String messageTemplate, final Object... args) {
//    LOGGER.log(level, MessageFormat.format(messageTemplate, args));
  }
  
  
  
  private MustReleaseLattice getLatticeFor(final IRNode node) {
    IRNode flowUnit = edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis.getFlowUnit(node);
    if (flowUnit == null)
      return null;
    final FlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[]> a = getAnalysis(flowUnit);
    final MustReleaseLattice mrl = (MustReleaseLattice) a.getLattice();
    return mrl;
  }

  
  @Override
  protected FlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[]> createAnalysis(final IRNode flowUnit) {
    final MustReleaseLattice mustReleaseLattice =
      MustReleaseLattice.createForFlowUnit(flowUnit, thisExprBinder, binder, jucLockUsageManager);    
    final FlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[]> analysis =
      new BackwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[]>(
          "Must Release Analysis", mustReleaseLattice,
          new MustReleaseTransfer(thisExprBinder, binder, lockUtils, mustReleaseLattice, nonNullAnalysis), DebugUnparser.viewer);
    return analysis;
  }

  /**
   * Get the matching unlock.
   * @return The IRNode of the matching unlock method call.
   */
  public Set<IRNode> getUnlocksFor(final IRNode mcall) {
    MethodCall call = (MethodCall) tree.getOperator(mcall);
    final ImmutableList<ImmutableSet<IRNode>>[] value = getAnalysisResultsAfter(mcall);
    final MustReleaseLattice mrl = getLatticeFor(mcall);
    
    log(Level.INFO, "getUnlocksFor({0} at {1}) == {2}",
        MethodCall.getMethod(mcall), JavaNode.getSrcRef(mcall).getLineNumber(),
        mrl.toString(value));
   
    final Set<IRNode> unlockCalls =
      mrl.getUnlocksFor(value, call.get_Object(mcall), thisExprBinder, binder);
    /* Remove ourself from the set---this will happen in the case of a 
     * tryLock() embedded in an if-statement, see 
     * MustReleaseTransfer.transferConditional().
     */
    if (unlockCalls != null) unlockCalls.remove(mcall);
    return unlockCalls;
  }
  
  
  
  static final class MustReleaseTransfer extends
      edu.uwm.cs.fluid.java.control.JavaBackwardTransfer<
          MustReleaseLattice, ImmutableList<ImmutableSet<IRNode>>[]> {
    private final ThisExpressionBinder thisExprBinder;
    private final LockUtils lockUtils;
    private final SimpleNonnullAnalysis nonNullAnalysis;

    public MustReleaseTransfer(
        final ThisExpressionBinder teb, final IBinder binder, final LockUtils lu,
        final MustReleaseLattice lattice, final SimpleNonnullAnalysis sna) {
      super(binder, lattice);
      thisExprBinder = teb;
      lockUtils = lu;
      nonNullAnalysis = sna;
    }

    /**
     * Is the method declared to return a lock?
     */
    private boolean isLockGetterMethod(final IRNode mcall) {
      final IRNode mdecl = binder.getBinding(mcall);
      final ReturnsLockPromiseDrop returnedLock = LockUtils.getReturnedLock(mdecl);
      return (returnedLock != null);
    }
    
    public ImmutableList<ImmutableSet<IRNode>>[] transferConditional(
        final IRNode node, final boolean flag, 
        final ImmutableList<ImmutableSet<IRNode>>[] after) {
      // Bail out if input is top or bottom
      if (!lattice.isNormal(after)) {
        return after;
      }
      final boolean logInfo = LOGGER.isLoggable(Level.INFO);
      ISrcRef ref = logInfo ? JavaNode.getSrcRef(node) : null;
      final String header = !logInfo ? "" : MessageFormat.format(
          "transferConditional({0} at {1}, {2}, {3})",
          DebugUnparser.toString(node), ref == null ? "?" :ref.getLineNumber(),
          flag, lattice.toString(after));
          
      /* We only do interesting things if node is a method call node for
       * a tryLock() call.
       */
      final Operator op = tree.getOperator(node);
      if (op instanceof MethodCall) {
        final LockMethods lockMethod = lockUtils.whichLockMethod(node);
        if (lockMethod == LockMethods.TRY_LOCK) {
          /* Here the problem is that transferCall() will be called after
           * transferConditional() and always acquire the lock.  So we must
           * push an unlock() on the stack so that the tryLock() will have an
           * unlock() to match up with.  (In a well-formed true-branch, the
           * programmer will have already made sure there is an unlock() call.  
           */
          if (!flag) { // else branch
            /* XXX: This is possibly sleazy: foundUnlock doesn't currently care
             * whether the given method is a lock or unlock, but if it does in
             * the future, we could have problems because we are giving it the
             * node for a tryLock() call.
             */
            final ImmutableList<ImmutableSet<IRNode>>[] newValue =
              lattice.foundUnlock(after, node, thisExprBinder, binder);
            if (logInfo) {
            	log(Level.INFO, "{0} == {1} [push fake unlock]",
            		header, lattice.toString(newValue));
            }
            return newValue;
          }
        }
      }
      
      // Normal case, always return what came into the conditional
      if (logInfo) {
    	  log(Level.INFO, "{0} == {1} [unchanged]",
    		  header, lattice.toString(after));
      }
      return after;
    }
    
    @Override
    protected ImmutableList<ImmutableSet<IRNode>>[] transferIsObject(
        final IRNode node, final boolean flag,
        final ImmutableList<ImmutableSet<IRNode>>[] value) {
      // Bail out if input is top or bottom
      if (!lattice.isNormal(value)) {
        return value;
      }
      
      final ISrcRef srcRef = JavaNode.getSrcRef(node);
      final String header = 
        MessageFormat.format("transferIsObject({0} at {1}, {2}, {3})",
        DebugUnparser.toString(node), srcRef == null ? 0 : srcRef.getLineNumber(),
        flag, lattice.toString(value));        
      if (!flag) {
        /* Abrupt case. Return BOTTOM if we can determine that the object must
         * not be null. We determine the object is non-null (1) if the object is
         * referenced by a final field that is initialized to (a) a new object
         * or (b) to the result of calling ReadWriteLock.readLock() or
         * ReadWriteLock.writeLock(); (2) if the object is a method call to a
         * method with a returns lock annotation; or (3) if the object is
         * referenced by a local variable that is known to be non-null.
         */
        final ImmutableList<ImmutableSet<IRNode>>[] bottom = lattice.bottom();
        final Operator operator = JJNode.tree.getOperator(node);
        if (FieldRef.prototype.includes(operator)) {
          final IRNode varDecl = binder.getBinding(node);
          if (TypeUtil.isFinal(varDecl)) { // Final field, check initialization
            final IRNode init = VariableDeclarator.getInit(varDecl);
            if (Initialization.prototype.includes(init)) {
              final IRNode initValue = Initialization.getValue(init);
              final Operator initValueOp = JJNode.tree.getOperator(initValue);
              // (1a) Initialized to new object
              if (NewExpression.prototype.includes(initValueOp) ||
                  AnonClassExpression.prototype.includes(initValueOp)) {
                log(Level.INFO, "{0} == {1} [not null by initialization: return BOTTOM]",
                    header, lattice.toString(bottom));
                return lattice.bottom();
              }
              
              /* (1b) Initialized to ReadWriteLock.readLock() or
               * ReadWriteLock.writeLock()
               */
              if (MethodCall.prototype.includes(initValueOp) &&
                  lockUtils.isJUCRWMethod(initValue)) {
                log(Level.INFO, "{0} == {1} [not null by initialization: return BOTTOM]",
                    header, lattice.toString(bottom));
                return lattice.bottom();
              }

            }
          }
        } else if (MethodCall.prototype.includes(operator)) {
          if (isLockGetterMethod(node) || lockUtils.isJUCRWMethod(node)) {
            // Lock getter methods do not return null values.
            log(Level.INFO, "{0} == {1} [not null by lock getter method: return BOTTOM]",
                header, lattice.toString(bottom));
            return bottom;
          }
        } else if (VariableUseExpression.prototype.includes(operator)) {
          final Set<IRNode> nonNull = nonNullAnalysis.getNonnullBefore(node);
          final IRNode varDecl = binder.getBinding(node);
          if (nonNull.contains(varDecl)) {
            log(Level.INFO, "{0} == {1} [not null by nonNullAnalysis: return BOTTOM]",
                header, lattice.toString(bottom));
            return bottom;
          }
        }
      }
      log(Level.INFO, "{0} == {1} [value unchanged]",
          header, lattice.toString(value));
      return value;
    }

    
    @Override
    protected ImmutableList<ImmutableSet<IRNode>>[] transferCall(
        final IRNode call, final boolean flag,
        final ImmutableList<ImmutableSet<IRNode>>[] value) {
      // Bail out if input is top or bottom
      if (!lattice.isNormal(value)) {
        return value;
      }
      
      final Operator op = tree.getOperator(call);
      if (op instanceof MethodCall) {
    	final boolean logInfo = LOGGER.isLoggable(Level.INFO);
    	ISrcRef ref = logInfo ? JavaNode.getSrcRef(call) : null;
        final String header = !logInfo ? null :
          MessageFormat.format("transferCall({0} at {1}, {2}, {3})",
              DebugUnparser.toString(call), ref == null ? "?" : ref.getLineNumber(),
              flag, lattice.toString(value));
        
        // Method call: check to see if it is a lock or an unlock
        final LockMethods lockMethod = lockUtils.whichLockMethod(call);
        if (lockMethod != LockMethods.NOT_A_LOCK_METHOD && lockMethod != LockMethods.IDENTICALLY_NAMED_METHOD) {
          if (lockMethod.isLock) {
            // For exceptional termination, the lock is not acquired
            if (flag) {
              final ImmutableList<ImmutableSet<IRNode>>[] newValue =
                lattice.foundLock(value, call, thisExprBinder, binder);
              if (logInfo) {
            	  log(Level.INFO, "{0} == {1} [pop lock stack]",
            			  header, lattice.toString(newValue));
              }
              return newValue;
            } else {
              if (logInfo) {
            	  log(Level.INFO, "{0} == {1} [abrupt termination, unchanged]",
            			  header, lattice.toString(value));
              }
              return value;
            }
          } else { // Must be unlock()
            // The lock is always released, even for abrupt termination.
            final ImmutableList<ImmutableSet<IRNode>>[] newValue =
              lattice.foundUnlock(value, call, thisExprBinder, binder);
            if (logInfo) {
            	log(Level.INFO, "{0} == {1} [push unlock call]",
            			header, lattice.toString(newValue));
            }
            return newValue;
          }
        } else {
          // Not a lock or an unlock; is it a lock getter call?
          if (isLockGetterMethod(call) || lockUtils.isJUCRWMethod(call)) {
            /* In the abrupt case, return bottom to indicate that lock getter
             * methods don't throw exceptions.
             */
            final ImmutableList<ImmutableSet<IRNode>>[] newValue = (flag ? value : lattice.bottom());
            if (logInfo) {
            	log(Level.INFO, "{0} == {1} [Lock getter method: {2}]",
            			header, newValue, flag ? "normal termination: unchanged" : "abrupt termination: bottom");
            }
            return newValue;
          } else {
            // Otherwise, not interesting
            return value;
          }
        }
      } else {
        // Constructor calls are not interesting
        return value;
      }
    }

    @Override
    protected FlowAnalysis<ImmutableList<ImmutableSet<IRNode>>[]> createAnalysis(IBinder binder) {
      return new BackwardAnalysis<ImmutableList<ImmutableSet<IRNode>>[]>(
          "Must Release Analysis", lattice, this, DebugUnparser.viewer);
    }

    public ImmutableList<ImmutableSet<IRNode>>[] transferComponentSink(IRNode node, boolean normal) {
      final ImmutableList<ImmutableSet<IRNode>>[] emptyValue = lattice.getEmptyValue();
      log(Level.INFO, "transferComponentSink({0}) == {1}",
          normal, lattice.toString(emptyValue));
      return emptyValue;
    }
  }
}

