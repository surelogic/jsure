package com.surelogic.analysis;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.surelogic.util.IRNodeIndexedArrayLattice;
import com.surelogic.util.IThunk;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.SimplifiedJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.AbstractRemovelessIterator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.cmu.cs.fluid.util.Triple;
import edu.uwm.cs.fluid.java.analysis.EvaluationStackLattice;
import edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.util.Lattice;
import edu.uwm.cs.fluid.util.PairLattice;
import edu.uwm.cs.fluid.util.UnionLattice;

/**
 * Specialized intraprocedural analysis whose flow analysis models an 
 * evaluation stack, additional arbitrary state, and also infers the state
 * of local variables.
 * 
 * <p>Primarily aggregates additional class declarations that are all
 * interrelated.
 * 
 * @param <I> The type of the state to be inferred for each local variable.
 * @param <R> The type of the overall value used by analysis.  
 * @param <L3> The lattice type of the inferred variable states.
 * @param <L5> The lattice type of the overall analysis.
 */
public abstract class StackEvaluatingAnalysisWithInference<
    I,
    R extends StackEvaluatingAnalysisWithInference.EvalValue<?, ?, I, ?>,
    L3 extends Lattice<I> & StackEvaluatingAnalysisWithInference.InferredHelper<I>,
    L5 extends StackEvaluatingAnalysisWithInference.EvalLattice<?, I, ?, R, ?, L3, ?>>
extends IntraproceduralAnalysis<R, L5, JavaForwardAnalysis<R, L5>> {
  protected StackEvaluatingAnalysisWithInference(final IBinder binder) {
    super(binder);
  }
  
  
  
  @Override
  protected abstract JavaForwardAnalysis<R, L5> createAnalysis(final IRNode flowUnit);

  
  
  // ======================================================================
  
  
  
  /**
   * Mixin interface for lattices for the inferred values.  Adds functionality
   * that allows the lattice for the array of inferred values to be encapsulated
   * here.
   */
  public static interface InferredHelper<I> {
    public I getEmptyElementValue();
    public I[] newArray(int size); // XXX: Kill this
  }
  
  
  
  public static final class Assignment<I>
  extends com.surelogic.common.Pair<IRNode, I> {
    public Assignment(final IRNode where, final I state) {
      super(where, state);
    }
    
    public IRNode getWhere() { return first(); }
    public I getState() { return second(); }
  }
  
  
  
  public static final class InferredPair<I>
  extends com.surelogic.common.Pair<I, ImmutableSet<Assignment<I>>> {
    public InferredPair(final I state, ImmutableSet<Assignment<I>> assignments) {
      super(state, assignments);
    }
    
    public I getInferred() { return first(); }
  }
  
  
  
  private static final class InferredPairLattice<I, L extends Lattice<I> & InferredHelper<I>>
  extends PairLattice<I, ImmutableSet<Assignment<I>>, L, UnionLattice<Assignment<I>>, InferredPair<I>> {
    public InferredPairLattice(final L l1) {
      super(l1, new UnionLattice<Assignment<I>>());
    }

    @Override
    protected InferredPair<I> newPair(
        final I v1, final ImmutableSet<Assignment<I>> v2) {
      return new InferredPair<I>(v1, v2);
    }
    
    public InferredPair<I> getEmptyElementValue() {
      return newPair(lattice1.getEmptyElementValue(),
          ImmutableHashOrderSet.<Assignment<I>>emptySet());
    }
    
    @SuppressWarnings("unchecked")
    public InferredPair<I>[] newArray(final int size) {
      // XXX: See if I can fix this in some elegant way
      return new InferredPair[size];
    }
    
    public L getInferredStateLattice() {
      return lattice1;
    }
    
    /**
     * Set the inferred state of a local variable at the given index.
     */
    public InferredPair<I> inferVar(
        final InferredPair<I> current, final I v, final IRNode src) {
      /* Here we are just adding the assignment to the set.  But I wonder 
       * if the correct thing to do is to JOIN the incoming state with any
       * existing state for the same assignment site.  So far this doesn't
       * seem to be a problem.
       */
      return newPair(
          lattice1.join(current.getInferred(), v),
          current.second().addCopy(new Assignment<I>(src, v)));
    }
  }
  
  
  
  /**
   * Lattice for array of inferred variable states.
   *
   * @param <I> The type of the state to be inferred for each local variable.
   * @param <L> The lattice type for the inferred values.
   */
  private static final class InferredLattice<I, L extends Lattice<I> & InferredHelper<I>>
  extends IRNodeIndexedArrayLattice<InferredPairLattice<I, L>, InferredPair<I>> {
    private final InferredPair<I>[] empty;
    
    public InferredLattice(final InferredPairLattice<I, L> base, final List<IRNode> keys) {
      super(base, keys);
      empty = createEmptyValue();
    }
    
    @Override
    protected InferredPair<I> getEmptyElementValue() {
      return baseLattice.getEmptyElementValue();
    }

    @Override
    public InferredPair<I>[] getEmptyValue() {
      return empty;
    }

    @Override
    protected void indexToString(final StringBuilder sb, final IRNode index) {
      final Operator op = JJNode.tree.getOperator(index);
      if (ParameterDeclaration.prototype.includes(op)) {
        sb.append(ParameterDeclaration.getId(index));
      } else { // VariableDeclarator
        sb.append(VariableDeclarator.getId(index));
      }
    }

    @Override
    protected InferredPair<I>[] newArray() {
      return baseLattice.newArray(size);
    }
    
    public final IRNode[] cloneKeys() {
      return indices.clone();
    }
    
    public L getInferredStateLattice() {
      return baseLattice.getInferredStateLattice();
    }
    
    /**
     * Set the inferred state of a local variable at the given index.
     */
    public final InferredPair<I>[] inferVar(
        final InferredPair<I>[] current, final int idx, final I v, final IRNode src) {
      return replaceValue(current, idx, baseLattice.inferVar(current[idx], v, src));
    }

  }
  
  
  
  /**
   * Pair value for the evaluation state: the first element is arbitrary 
   * analysis state, and the second element is an array of inferred states
   * for local variables.
   *
   * @param <S> The type of the arbitrary analysis state.
   * @param <I> The type of the state to be inferred for each local variable.
   */
  public static abstract class StatePair<S, I>
  extends com.surelogic.common.Pair<S, InferredPair<I>[]> {
    public StatePair(final S s, final InferredPair<I>[] i) {
      super(s, i);
    }
  }
  
  
  
  /**
   * Lattice type for the state component of the evaluation. 
   *
   * @param <S> The type of the arbitrary analysis state.
   * @param <I> The type of the state to be inferred for each local variable.
   * @param <V> The type of overall state object used during evaluation.
   * @param <L1> The lattice type for the arbitrary analysis state.
   * @param <L2> The lattice type for the inferred variable states.
   */
  public static abstract class StatePairLattice<
      S, I, V extends StatePair<S, I>,
      L1 extends Lattice<S>,
      L2 extends Lattice<I> & InferredHelper<I>>
  extends PairLattice<S, InferredPair<I>[], L1, InferredLattice<I, L2>, V> {
    protected StatePairLattice(final L1 l1, final L2 l2, final List<IRNode> keys) {
      super(l1, new InferredLattice<I, L2>(
          new InferredPairLattice<I, L2>(l2), keys));
    }
    
    
    
    public L2 getInferredStateLattice() {
      return lattice2.getInferredStateLattice();
    }

    public IRNode[] getInferredStateKeys() {
      return lattice2.cloneKeys();
    }
    
    public final InferredPair<I>[] getEmptyInferredValue() {
      return lattice2.getEmptyValue();
    }
    
    /**
     * Get the array index of the given variable in the array of inferred
     * variables.
     * @return The array index of the variable, or -1 if the variable is not 
     * in the array.
     */
    public final int indexOfInferred(final IRNode var) {
      return lattice2.indexOf(var);
    }
    
    /**
     * Set the inferred state of a local variable at the given index.
     */
    public final V inferVar(final V state, final int idx, final I v, final IRNode src) {
      return newPair(state.first(), lattice2.inferVar(state.second(), idx, v, src));
    }
  }

  
  
  // ======================================================================


  
  /**
   * Value type for the overall value pushed around by the analysis.
   * First component is the evaluation stack, and the second component
   * is a pair of arbitrary stack, and an array of inferred local variable
   * states.
   * 
   * @param <T> The type of stack elements.
   * @param <S> The type of the arbitrary analysis state.
   * @param <I> The type of the state to be inferred for each local variable.
   * @param <P> The overall type of the state component for the EvaluationStackLattice.
   */
  public static abstract class EvalValue<T, S, I, P extends StatePair<S, I>>
  extends EvaluationStackLattice.EvalPair<T, P> {
    protected EvalValue(final ImmutableList<T> v1, final P v2) {
      super(v1, v2);
    }
  }



  /**
   * Specialization of {@link EvaluationStackLattice} where the "state" 
   * component of the value is itself a pair, where  the first component is 
   * arbitrary state, and the second component is an array of values meant 
   * to correspond to inferred state for local variables.
   * 
   * @param <V> The type of the stack elements.
   * @param <I> The type of the state to be inferred for each local variable
   * @param <SS> The type of overall state object used during evaluation.
   * @param <R> The type of the overall value used by analysis.  
   * @param <L1> The lattice type of the stack elements.
   * @param <L3> The lattice type of the inferred variable states.
   * @param <L4> The lattice type of the overall state object.
   */
  public static abstract class EvalLattice<
      V,
      I,
      SS extends StackEvaluatingAnalysisWithInference.StatePair<?, I>,
      R extends StackEvaluatingAnalysisWithInference.EvalValue<V, ?, I, SS>,
      L1 extends Lattice<V>,
      L3 extends Lattice<I> & InferredHelper<I>,
      L4 extends StackEvaluatingAnalysisWithInference.StatePairLattice<?, I, SS, ?, L3>>
  extends EvaluationStackLattice<V, SS, L1, L4, R> {
    protected EvalLattice(final L1 l1, final L4 l2) {
      super(l1, l2);
    }
   
    
    
    public L3 getInferredStateLattice() {
      return lattice2.getInferredStateLattice();
    }

    public IRNode[] getInferredStateKeys() {
      return lattice2.getInferredStateKeys();
    }
    
    public final int indexOfInferred(final IRNode var) {
      return lattice2.indexOfInferred(var);
    }
    
    public final R inferVar(final R v, final int idx, final I e, final IRNode src) {
      return newPair(v.first(), lattice2.inferVar(v.second(), idx, e, src));
    }
  }



  // ======================================================================

  
  
  public static final class InferredVarStateQuery<
      I, L1 extends Lattice<I> & InferredHelper<I>, V extends EvalValue<?, ?, I, ?>,
      L2 extends StackEvaluatingAnalysisWithInference.EvalLattice<?, I, ?, V, ?, L1, ?>>
  extends SimplifiedJavaFlowAnalysisQuery<InferredVarStateQuery<I, L1, V, L2>, Result<I, L1>, V, L2> {
    protected InferredVarStateQuery(final IThunk<? extends IJavaFlowAnalysis<V, L2>> thunk) {
      super(thunk);
    }
    
    protected InferredVarStateQuery(final Delegate<InferredVarStateQuery<I, L1, V, L2>, Result<I, L1>, V, L2> d) {
      super(d);
    }
    
    @Override
    protected final RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }

    @Override
    protected final Result<I, L1> processRawResult(
        final IRNode expr, final L2 lattice, final V rawResult) {
      return new Result<I, L1>(
          lattice.getInferredStateKeys(),
          rawResult.second().second(),
          lattice.getInferredStateLattice());
    }

    @Override
    protected InferredVarStateQuery<I, L1, V, L2> newSubAnalysisQuery(
        final Delegate<InferredVarStateQuery<I, L1, V, L2>, Result<I, L1>, V, L2> delegate) {
      return new InferredVarStateQuery<I, L1, V, L2>(delegate);
    }
  }

  

  /**
   * Abstract result type for the the "inferred variables query"
   * {@link InferredVarStateQuery}.
   *
   * @param <I> The type of the state to be inferred for each local variable.
   */
  public static final class Result<I, L extends Lattice<I>>
  implements Iterable<Triple<IRNode, I, Set<Assignment<I>>>> {
    private final L inferredStateLattice;
    private final IRNode[] keys;
    private final InferredPair<I>[] values;
    
    protected Result(final IRNode[] keys, final InferredPair<I>[] val, final L sl) {
      this.keys = keys;
      this.values = val;
      this.inferredStateLattice = sl;
    }
    
    @Override
    public final Iterator<Triple<IRNode, I, Set<Assignment<I>>>> iterator() {
      return new AbstractRemovelessIterator<Triple<IRNode, I, Set<Assignment<I>>>>() {
        private int idx = 0;
        
        @Override
        public boolean hasNext() {
          return idx < keys.length;
        }

        @Override
        public Triple<IRNode, I, Set<Assignment<I>>> next() {
          final int currentIdx = idx++;
          final InferredPair<I> inferredPair = values[currentIdx];
          return new Triple<IRNode, I, Set<Assignment<I>>>(
              keys[currentIdx],
              inferredPair.getInferred(), inferredPair.second());
        }
      };
    }
    
    public L getLattice() {
      return inferredStateLattice;
    }

    public final boolean lessEq(final I a, final I b) {
      return inferredStateLattice.lessEq(a, b);
    }
  }
  
  
  
  public final InferredVarStateQuery<I, L3, R, L5>
  getInferredVarStateQuery(final IRNode flowUnit) {
    return new InferredVarStateQuery<I, L3, R, L5>(getAnalysisThunk(flowUnit));
  }
}
