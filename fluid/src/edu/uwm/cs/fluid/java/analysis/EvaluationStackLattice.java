package edu.uwm.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.util.ImmutableList;
import edu.uwm.cs.fluid.java.control.JavaEvaluationTransfer;
import edu.uwm.cs.fluid.util.Lattice;
import edu.uwm.cs.fluid.util.ListLattice;
import edu.uwm.cs.fluid.util.PairLattice;

/**
 * Abstract lattice for analyses, most likely based around
 * {@link JavaEvaluationTransfer} that need to keep track of the evaluation
 * stack.  Based around a pair: The first element is the evaluation stack,
 * and the second element is any additional state needed by the analysis.
 * @param <V> The type of the stack elements.
 * @param <S> The type of the additional analysis state.
 * @param <L1> The lattice type of the stack elements.
 * @param <L2> The lattice type of the additional analysis state.
 * @param <R> The type of the overall lattice values (R for "Result").
 */
public abstract class EvaluationStackLattice<V, S,
    L1 extends Lattice<V>,
    L2 extends Lattice<S>,
    R extends EvaluationStackLattice.Pair<V, S>>
extends PairLattice<ImmutableList<V>, S, ListLattice<L1, V>, L2, R>
implements JavaEvaluationOperations<R, V> {
  /**
   * @param <V> The type of the stack elements.
   * @param <S> The type of the additional analysis state.
   */
  public static abstract class Pair<V, S> extends com.surelogic.common.Pair<ImmutableList<V>, S> {
    public Pair(final ImmutableList<V> v, final S s) {
      super(v, s);
    }
  }
  
  
  
  protected EvaluationStackLattice(final ListLattice<L1, V> l1, final L2 l2) {
    super(l1, l2);
  }

  
  
  /**
   * Is the value normal?  If not, there has been an analysis error.
   * The value is normal if the stack is valid list.
   */
  public final boolean isNormal(final R val) {
    return lattice1.isList(val.first());
  }

  @Override
  public final R pop(final R val) {
    return newPair(lattice1.pop(val.first()), val.second());
  }
  
  @Override
  public final R push(final R val, final V v) {
    return newPair(lattice1.push(val.first(), v), val.second());
  }
  
  @Override
  public final V peek(final R val) {
    return lattice1.peek(val.first());
  }
  
  @Override
  public final R popAllPending(final R val, final int stackFloorSize) {
    if (stackFloorSize == 0) {
      return newPair(ImmutableList.<V>nil(), val.second());
    } else {
      ImmutableList<V> newStack = val.first();
      while (newStack.size() > stackFloorSize) {
        newStack = lattice1.pop(newStack);
      }
      return newPair(newStack, val.second());
    }
  }
  
  @Override
  public final R popSecond(final R val) {
    final V v = lattice1.peek(val.first());
    return newPair(
        lattice1.push(lattice1.pop(lattice1.pop(val.first())), v),
        val.second());
  }
  
  @Override
  public final R dup(final R val) {
    return push(val, lattice1.peek(val.first()));
  }
}
