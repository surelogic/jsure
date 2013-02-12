package com.surelogic.analysis;

import java.util.Iterator;
import java.util.List;

import com.surelogic.common.Pair;
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
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.uwm.cs.fluid.java.analysis.EvaluationStackLattice;
import edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis;
import edu.uwm.cs.fluid.java.control.IJavaFlowAnalysis;
import edu.uwm.cs.fluid.java.control.JavaForwardAnalysis;
import edu.uwm.cs.fluid.util.Lattice;
import edu.uwm.cs.fluid.util.PairLattice;

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
   * Pair value for the evaluation state: the first element is arbitrary 
   * analysis state, and the second element is an array of inferred states
   * for local variables.
   *
   * @param <S> The type of the arbitrary analysis state.
   * @param <I> The type of the state to be inferred for each local variable.
   */
  public static abstract class StatePair<S, I>
  extends com.surelogic.common.Pair<S, I[]> {
    public StatePair(final S s, final I[] i) {
      super(s, i);
    }
  }
  
  
  
  /**
   * Mixin interface for lattices for the inferred values.  Adds functionality
   * that allows the lattice for the array of inferred values to be encapsulated
   * here.
   */
  public static interface InferredHelper<I> {
    public I getEmptyElementValue();
    public I[] newArray(int size);
  }
  
  
  
  /**
   * Lattice for array of inferred variable states.
   *
   * @param <I> The type of the state to be inferred for each local variable.
   * @param <L> The lattice type for the inferred values.
   */
  private static final class InferredLattice<I, L extends Lattice<I> & InferredHelper<I>>
  extends IRNodeIndexedArrayLattice<L, I> {
    private final I[] empty;
    
    public InferredLattice(final L base, final List<IRNode> keys) {
      super(base, keys);
      empty = createEmptyValue();
    }
    
    @Override
    protected I getEmptyElementValue() {
      return baseLattice.getEmptyElementValue();
    }

    @Override
    public I[] getEmptyValue() {
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
    protected I[] newArray() {
      return baseLattice.newArray(size);
    }
  }
  
  
  
  /**
   * Lattice type for the state component of the evaluation. 
   *
   * @param <S> The type of the arbitrary analysis state.
   * @param <I> The type of the state to be inferred for each local variable.
   * @param <V> The type of overall state object used during evaluation.
   * @param <L1> The lattice type for the arbitrary analysis state.
   * @param <L2> The lattice type for the variable states.
   */
  public static abstract class StatePairLattice<
      S, I, V extends StatePair<S, I>,
      L1 extends Lattice<S>,
      L2 extends Lattice<I> & InferredHelper<I>>
  extends PairLattice<S, I[], L1, InferredLattice<I, L2>, V> {
    protected StatePairLattice(final L1 l1, final L2 l2, final List<IRNode> keys) {
      super(l1, new InferredLattice<I, L2>(l2, keys));
    }
    
    
    
    public final InferredLattice<I, L2> getInferredLattice() {
      return lattice2;
    }
    
    
    
    public final I[] getEmptyInferredValue() {
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
    public final V inferVar(final V state, final int idx, final I v) {
      final I[] inferredArray = state.second();
      final I current = inferredArray[idx];
      final I joined = lattice2.getBaseLattice().join(current, v);
      return newPair(state.first(),
          lattice2.replaceValue(inferredArray, idx, joined));
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
    
    
    
    private final InferredLattice<I, L3> getInferredLattice() {
      return lattice2.getInferredLattice();
    }
    
    
    
    public final int indexOfInferred(final IRNode var) {
      return lattice2.indexOfInferred(var);
    }
    
    public final R inferVar(final R v, final int idx, final I e) {
      return newPair(v.first(), lattice2.inferVar(v.second(), idx, e));
    }
  }



  // ======================================================================

  
  
  public static abstract class InferredVarStateQuery<
      SELF extends InferredVarStateQuery<SELF, I, L1, V, R, L2>, 
      I, L1 extends Lattice<I> & InferredHelper<I>, V extends EvalValue<?, ?, I, ?>, R extends InferredResult<I, L1>,
      L2 extends StackEvaluatingAnalysisWithInference.EvalLattice<?, I, ?, V, ?, L1, ?>>
  extends SimplifiedJavaFlowAnalysisQuery<SELF, R, V, L2> {
    protected InferredVarStateQuery(final IThunk<? extends IJavaFlowAnalysis<V, L2>> thunk) {
      super(thunk);
    }
    
    protected InferredVarStateQuery(final Delegate<SELF, R, V, L2> d) {
      super(d);
    }
    
    @Override
    protected final RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }

    @Override
    protected final R processRawResult(
        final IRNode expr, final L2 lattice, final V rawResult) {
      return makeInferredResult(
          lattice.getInferredLattice(), rawResult.second().second());
    }

    protected abstract R makeInferredResult(IRNodeIndexedArrayLattice<L1, I> lattice, I[] inferredVars);
  }

  

  /**
   * Abstract result type for the the "inferred variables query"
   * {@link InferredVarStateQuery}.
   *
   * @param <I> The type of the state to be inferred for each local variable.
   */
  public static abstract class InferredResult<I, L extends Lattice<I>>
  implements Iterable<Pair<IRNode, I>> {
    protected final IRNodeIndexedArrayLattice<L, I> lattice;
    protected final I[] values;
    
    protected InferredResult(final IRNodeIndexedArrayLattice<L, I> lat, final I[] val) {
      lattice = lat;
      values = val;
    }
    
    @Override
    public final Iterator<Pair<IRNode, I>> iterator() {
      return new AbstractRemovelessIterator<Pair<IRNode, I>>() {
        private int idx = 0;
        
        @Override
        public boolean hasNext() {
          return idx < lattice.getSize();
        }

        @Override
        public Pair<IRNode, I> next() {
          final int currentIdx = idx++;
          return new Pair<IRNode, I>(
              lattice.getKey(currentIdx), values[currentIdx]);
        }
      };
    }

    public final boolean lessEq(final I a, final I b) {
      return lattice.getBaseLattice().lessEq(a, b);
    }
  }
  
  
  
  public abstract InferredVarStateQuery<?, I, L3, R, ? extends InferredResult<I, L3>, L5>
  getInferredVarStateQuery(IRNode flowUnit);  
}
