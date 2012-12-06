/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/AbstractLockStackLattice.java,v 1.18 2008/01/19 00:14:21 aarong Exp $*/
package com.surelogic.analysis.concurrency.heldlocks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.concurrency.heldlocks.locks.HeldLock;
import com.surelogic.common.ref.IJavaRef;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IndependentIRNode;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.SynchronizedStatement;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.uwm.cs.fluid.util.AssociativeArrayLattice;
import edu.uwm.cs.fluid.util.ListLattice;
import edu.uwm.cs.fluid.util.UnionLattice;

/**
 * Abstract super class that holds the common functionality for the lattices
 * used by Must-Release and Must-Hold analysis. The lattice is a map from
 * {@link Lock} objects to a stack of method call IRNodes. For Must-Release
 * analysis, the method calls are unlock() method calls; for Must-Hold analysis
 * the method calls are lock() method calls. The map is maintained as an
 * ArrayLattice, where the Locks are mapped to array locations; this mapping is
 * referenced using the {@link #getIndexOf} method. The domain of the lattice is
 * {@code ImmutableList<ImmutableSet<IRNode>>[]}.
 * 
 * @author aarong
 */
abstract class AbstractLockStackLattice extends
    AssociativeArrayLattice<HeldLock, ListLattice<UnionLattice<IRNode>, ImmutableSet<IRNode>>, ImmutableList<ImmutableSet<IRNode>>> {
  /**
   * In order to keep our analysis transfer functions strict, we need to create
   * a dummy stack value that we push on all the stacks.
   * 
   * @see #IGNORE_ME_SINGLETON_SET
   */
  protected static final IRNode IGNORE_ME = new IndependentIRNode();
  static {
    JJNode.setInfo(IGNORE_ME, "<ignore>");
  }

  /**
   * Singleton set of the {@link #IGNORE_ME bogus method call} that we must push
   * onto each lock stack so that we can differentiate an empty value from
   * bottom.
   */
  protected static final ImmutableSet<IRNode> IGNORE_ME_SINGLETON_SET = ImmutableHashOrderSet.<IRNode> emptySet()
      .addCopy(IGNORE_ME);

  protected final IBinder binder;
  protected final ThisExpressionBinder thisExprBinder;

  /**
   * Map from lock expressions to the set of locks that they resolve to.
   */
  protected final Map<IRNode, Set<HeldLock>> lockExprsToLockSets;

  /**
   * Protected constructor. Subclasses are expected to have a private
   * constructor and a static factory method called {@code createForFlowUnit} to
   * create instances of this class.
   * 
   * @param lockExprs
   *          The list of unique lock expressions that represent the domain of
   *          the map portion of this lattice.
   */
  @SuppressWarnings("unchecked")
  protected AbstractLockStackLattice(final ThisExpressionBinder teb, final IBinder b, final HeldLock[] lks,
      final Map<IRNode, Set<HeldLock>> map) {
    super(new ListLattice<UnionLattice<IRNode>, ImmutableSet<IRNode>>(new UnionLattice<IRNode>()), ImmutableList.NO_LISTS, lks);
    binder = b;
    thisExprBinder = teb;
    lockExprsToLockSets = map;
  }

  protected static HeldLock[] constructLockArray(final Map<IRNode, Set<HeldLock>> map, final Set<HeldLock> req,
      final Set<HeldLock> singleThreaded, final Set<HeldLock> classInit, final ThisExpressionBinder thisExprBinder,
      final IBinder binder) {
    /*
     * Build the List of locks. An O(n^2) operation because we do not want to
     * include aliases.
     */
    // (1) Init list with lock preconditions
    final List<HeldLock> lockList = new LinkedList<HeldLock>(req);
    /*
     * (2) Add locks due to single threadedness. Locks cannot be duplicates of
     * locks from (1) because constructors cannot requires locks on the
     * receiver.
     */
    lockList.addAll(singleThreaded);
    // (3) Add locks from lock expressions in the body
    for (final Set<HeldLock> lockSet : map.values()) {
      for (final HeldLock wantToAdd : lockSet) {
        // search for aliases
        boolean aliased = false;
        for (final HeldLock current : lockList) {
          if (current.mustAlias(wantToAdd, thisExprBinder, binder)) {
            aliased = true;
            break;
          }
        }
        if (!aliased) {
          lockList.add(wantToAdd);
        }
      }
    }
    // (4) Static locks from class initialization
    for (final HeldLock wantToAdd : classInit) {
      // search for aliases
      boolean aliased = false;
      for (final HeldLock current : lockList) {
        if (current.mustAlias(wantToAdd, thisExprBinder, binder)) {
          aliased = true;
          break;
        }
      }
      if (!aliased) {
        lockList.add(wantToAdd);
      }
    }
    HeldLock[] lockArray = HeldLock.NO_HELD_LOCKS;
    lockArray = lockList.toArray(lockArray);
    return lockArray;
  }

  protected static HeldLock[] getLocksFromMap(final Map<IRNode, Set<HeldLock>> map, final ThisExpressionBinder thisExprBinder,
      final IBinder binder) {
    /*
     * Build the List of locks. An O(n^2) operation because we do not want to
     * include aliases.
     */
    final List<HeldLock> lockList = new LinkedList<HeldLock>();
    for (final Set<HeldLock> lockSet : map.values()) {
      for (final HeldLock wantToAdd : lockSet) {
        // search for aliases
        boolean aliased = false;
        for (final HeldLock current : lockList) {
          if (current.mustAlias(wantToAdd, thisExprBinder, binder)) {
            aliased = true;
            break;
          }
        }
        if (!aliased) {
          lockList.add(wantToAdd);
        }
      }
    }
    HeldLock[] lockArray = HeldLock.NO_HELD_LOCKS;
    lockArray = lockList.toArray(lockArray);
    return lockArray;
  }

  // ======================================================================
  // == Associative array methods
  // ======================================================================

  @Override
  protected final boolean indexEquals(final HeldLock lock1, final HeldLock lock2) {
    return lock1.mustAlias(lock2, thisExprBinder, binder);
  }

  /**
   * Get a new empty value the lattice.
   */
  @SuppressWarnings("unchecked")
  @Override
  public final ImmutableList<ImmutableSet<IRNode>>[] getEmptyValue() {
    final ImmutableList<ImmutableSet<IRNode>>[] empty = new ImmutableList[size];
    // Push the singleton set with the bogus method call onto the stack
    final ImmutableList<ImmutableSet<IRNode>> initValue = ImmutableList.cons(IGNORE_ME_SINGLETON_SET,
        ImmutableList.<ImmutableSet<IRNode>> nil());
    for (int i = 0; i < empty.length; i++) {
      empty[i] = initValue;
    }
    return empty;
  }

  @Override
  public final boolean isNormal(final ImmutableList<ImmutableSet<IRNode>>[] value) {
    return value != bottom() && value != top();
  }

  /**
   * Push the given method call onto the stack. The affected locks are derived
   * from the receiver of the method call.
   * 
   * @param oldValue
   * @param call
   * @param binder
   * @return
   */
  protected final ImmutableList<ImmutableSet<IRNode>>[] pushCall(final ImmutableList<ImmutableSet<IRNode>>[] oldValue,
      final IRNode call) {
    MethodCall mcall = (MethodCall) JJNode.tree.getOperator(call);

    // Get the set of locks for the lock expression
    final IRNode lockExpr = mcall.get_Object(call);
    return pushLockExpression(oldValue, lockExpr, call);
  }

  public ImmutableList<ImmutableSet<IRNode>>[] pushLockExpression(final ImmutableList<ImmutableSet<IRNode>>[] oldValue,
      final IRNode lockExpr, final IRNode lockAction) {
    final Set<HeldLock> lockSet = lockExprsToLockSets.get(lockExpr);
    ImmutableList<ImmutableSet<IRNode>>[] result = oldValue;
    if (lockSet != null) {
      for (final HeldLock lock : lockSet) {
        final int idx = indexOf(lock);
        if (idx != -1) {
          // Push the method call onto the stack as a new singleton set
          result = replaceValue(result, idx,
              getBaseLattice().push(result[idx], ImmutableHashOrderSet.<IRNode> emptySet().addCopy(lockAction)));
        }
      }
    }
    return result;
  }

  /**
   * Remove the top method call based on the given lock method call. The
   * affected locks are derived from the receiver of the method call.
   * 
   * @param oldValue
   * @param call
   * @param binder
   * @return
   */
  protected final ImmutableList<ImmutableSet<IRNode>>[] popCall(final ImmutableList<ImmutableSet<IRNode>>[] oldValue,
      final IRNode call) {
    MethodCall mcall = (MethodCall) JJNode.tree.getOperator(call);

    // Get the set of locks for the lock expression
    final IRNode lockExpr = mcall.get_Object(call);
    return popLockExpression(oldValue, lockExpr);
  }

  public ImmutableList<ImmutableSet<IRNode>>[] popLockExpression(final ImmutableList<ImmutableSet<IRNode>>[] oldValue,
      final IRNode lockExpr) {
    final Set<HeldLock> lockSet = lockExprsToLockSets.get(lockExpr);
    ImmutableList<ImmutableSet<IRNode>>[] result = oldValue;
    if (lockSet != null) {
      for (final HeldLock lock : lockSet) {
        final int idx = indexOf(lock);
        if (idx != -1) {
          /*
           * If the stack is empty (size == 1 because of the bogus value), then
           * don't do anything. This prevents the lattice from getting screwed
           * up. Could return bottom instead, but that would screw up matching
           * for any valid pairs that come after this missed pairing.
           */
          if (result[idx].size() == 1) {
            return result;
          } else {
            // Replace the stack at the index with the popped version of the
            // stack
            result = replaceValue(result, idx, getBaseLattice().pop(result[idx]));
          }
        }
      }
    }
    return result;
  }

  /**
   * Get the the most recent set of calls for the given lock.
   * 
   * @return The set of calls at the top of stack for the given lock, or
   *         {@code null} if the top value is the poisoned TOP set value.
   */
  protected final Set<IRNode> peek(final ImmutableList<ImmutableSet<IRNode>>[] value, final HeldLock lock) {
    final int idx = indexOf(lock);
    if (idx != -1) {
      /*
       * Special case: if the stack is empty, return the empty set; If we try to
       * peek on the empty stack we return TOP resulting in poisoning.
       */
      if (value[idx].isEmpty()) {
        return Collections.emptySet();
      } else {
        final ImmutableSet<IRNode> peekedValue = getBaseLattice().peek(value[idx]);
        if (getBaseLattice().getBaseLattice().top().equals(peekedValue)) {
          return null;
        } else {
          return peekedValue;
        }
      }
    } else {
      return Collections.emptySet();
    }
  }

  /**
   * Get the the most recent set of calls for the given lock expression.
   * 
   * @return The set of calls at the top of stack for the given lock, or
   *         {@code null} if the top value is the poisoned TOP set value.
   */
  protected final Set<IRNode> peek(final ImmutableList<ImmutableSet<IRNode>>[] value, final IRNode lockExpr) {
    final Set<IRNode> result = new HashSet<IRNode>();
    for (final HeldLock lock : lockExprsToLockSets.get(lockExpr)) {
      final Set<IRNode> lockSet = peek(value, lock);
      if (lockSet == null)
        return null;
      result.addAll(lockSet);
    }
    // take out the bogus node
    result.remove(IGNORE_ME);
    return result;
  }

  @Override
  protected String toStringPrefixSeparator() {
    return "\n";
  }

  @Override
  protected String toStringOpen() {
    return "";
  }

  @Override
  protected String toStringSeparator() {
    return "\n";
  }

  @Override
  protected String toStringConnector() {
    return " -> ";
  }

  @Override
  protected String toStringClose() {
    return "\n";
  }

  @Override
  protected final void indexToString(final StringBuilder sb, final HeldLock lock) {
    sb.append(lock.toString());
  }

  @Override
  protected final void valueToString(final StringBuilder sb, final ImmutableList<ImmutableSet<IRNode>> stack) {
    if (stack == null) {
      sb.append("NULL STACK");
    } else {
      for (final Iterator<ImmutableSet<IRNode>> stackIter = stack.iterator(); stackIter.hasNext();) {
        final ImmutableSet<IRNode> set = stackIter.next();
        if (set == null) {
          sb.append("NULL");
        } else {
          // unparse set
          sb.append('{');
          for (final Iterator<IRNode> setIter = set.iterator(); setIter.hasNext();) {
            final IRNode call = setIter.next();
            if (call.equals(IGNORE_ME)) {
              // Node is a bogus place holder used by analysis
              sb.append("IGNORE_ME");
            } else
              try {
                final Operator op = JJNode.tree.getOperator(call);
                if (MethodCall.prototype.includes(op)) {
                  sb.append(MethodCall.getMethod(call));
                } else if (MethodDeclaration.prototype.includes(op)) {
                  sb.append("decl(");
                  sb.append(MethodDeclaration.getId(call));
                  sb.append(")");
                } else if (ConstructorDeclaration.prototype.includes(op)) {
                  sb.append("decl(");
                  sb.append(ConstructorDeclaration.getId(call));
                  sb.append(")");
                } else if (SynchronizedStatement.prototype.includes(op)) {
                  sb.append("sync");
                }
                sb.append('@');
                final IJavaRef javaRef = JavaNode.getJavaRef(call);
                if (javaRef == null || javaRef.getLineNumber() == -1)
                  sb.append("unknown");
                else {
                  sb.append(javaRef.getLineNumber());
                }
              } catch (final SlotUndefinedException e) {
                // Node is a bogus place holder used by analysis
                sb.append("IGNORE_ME");
              }
            if (setIter.hasNext()) {
              sb.append(", ");
            }
          }
          sb.append('}');
        }
        if (stackIter.hasNext()) {
          sb.append("::");
        }
      }
    }
  }
}
