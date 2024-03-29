package com.surelogic.analysis;

import java.util.Iterator;
import java.util.List;

import com.surelogic.common.Pair;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.util.IRNodeIndexedArrayLattice;
import com.surelogic.util.IThunk;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.SimplifiedJavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
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
 * @param <T> The type of the overall value used by analysis.  
 * @param <L_I> The lattice type of the inferred variable states.
 * @param <L_T> The lattice type of the overall analysis.
 */
public abstract class StackEvaluatingAnalysisWithInference<
    I,
    T extends StackEvaluatingAnalysisWithInference.EvalValue<?, ?, I, ?>,
    L_I extends StackEvaluatingAnalysisWithInference.InferredLattice<I, ? extends Lattice<I>>,
    L_T extends StackEvaluatingAnalysisWithInference.EvalLattice<?, I, ?, T, ?, L_I, ?>>
extends IntraproceduralAnalysis<T, L_T, JavaForwardAnalysis<T, L_T>> {
  protected StackEvaluatingAnalysisWithInference(final IBinder binder) {
    super(binder);
  }
  
  
  
  @Override
  protected abstract JavaForwardAnalysis<T, L_T> createAnalysis(final IRNode flowUnit);

  
  
  // ======================================================================
  
    
  
  /**
   * Lattice for array of inferred variable states.
   *
   * @param <I> The type of the state to be inferred for each local variable.
   * @param <L> The lattice type for the inferred values.
   */
  public static abstract class InferredLattice<I, L extends Lattice<I>>
  extends IRNodeIndexedArrayLattice<L, I> {
    private final I[] empty;
    
    public InferredLattice(final L base, final List<IRNode> keys) {
      super(base, keys);
      empty = createEmptyValue();
    }
    
    @Override
    protected final I getEmptyElementValue() {
      return baseLattice.bottom();
    }

    @Override
    public final I[] getEmptyValue() {
      return empty;
    }

    @Override
    protected final void indexToString(final StringBuilder sb, final IRNode index) {
      final Operator op = JJNode.tree.getOperator(index);
      if (ParameterDeclaration.prototype.includes(op)) {
        sb.append(ParameterDeclaration.getId(index));
      } else { // VariableDeclarator
        sb.append(VariableDeclarator.getId(index));
      }
    }
    
    public final IRNode[] cloneKeys() {
      return indices.clone();
    }
    
    /**
     * Set the inferred state of a local variable at the given index.
     */
    public final I[] inferVar(
        final I[] current, final int idx, final I v, final IRNode src) {
      return replaceValue(current, idx, baseLattice.join(current[idx], v));
    }
  }
  
  
  
  /**
   * Pair value for the evaluation state.
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
   * Lattice type for the state component of the evaluation. 
   *
   * @param <S> The type of the arbitrary analysis state.
   * @param <I> The type of the state to be inferred for each local variable.
   * @param <V> The type of overall state object used during evaluation.
   * @param <L_S> The lattice type for the arbitrary analysis state.
   * @param <L_I> The lattice type for the inferred variable states.
   */
  public static abstract class StatePairLattice<
      S, I, V extends StatePair<S, I>,
      L_S extends Lattice<S>,
      L_I extends InferredLattice<I, ? extends Lattice<I>>>
  extends PairLattice<S, I[], L_S, L_I, V> {
    protected StatePairLattice(final L_S l1, final L_I l2) {
      super(l1, l2);
    }
    
    
    
    public final IRNode[] getInferredStateKeys() {
      return lattice2.cloneKeys();
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
   * @param <E> The type of stack elements.
   * @param <S> The type of the arbitrary analysis state.
   * @param <I> The type of the state to be inferred for each local variable.
   * @param <V> The overall type of the state component for the EvaluationStackLattice.
   */
  public static abstract class EvalValue<E, S, I, V extends StatePair<S, I>>
  extends EvaluationStackLattice.EvalPair<E, V> {
    protected EvalValue(final ImmutableList<E> v1, final V v2) {
      super(v1, v2);
    }
  }



  /**
   * Specialization of {@link EvaluationStackLattice} for use with
   * {@link EvalValue}.
   * 
   * @param <E> The type of the stack elements.
   * @param <I> The type of the state to be inferred for each local variable
   * @param <V> The type of overall state object used during evaluation.
   * @param <R> The type of the overall value used by analysis.  
   * @param <L_E> The lattice type of the stack elements.
   * @param <L_I> The lattice type of the inferred variable states.
   * @param <L_V> The lattice type of the overall state object.
   */
  public static abstract class EvalLattice<
      E,
      I,
      V extends StackEvaluatingAnalysisWithInference.StatePair<?, I>,
      R extends StackEvaluatingAnalysisWithInference.EvalValue<E, ?, I, V>,
      L_E extends Lattice<E>,
      L_I extends InferredLattice<I, ? extends Lattice<I>>,
      L_V extends StackEvaluatingAnalysisWithInference.StatePairLattice<?, I, V, ?, L_I>>
  extends EvaluationStackLattice<E, V, L_E, L_V, R> {
    protected EvalLattice(final L_E l1, final L_V l2) {
      super(l1, l2);
    }
   
    
    
    public final IRNode[] getInferredStateKeys() {
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

  
  
  public static abstract class InferredVarStateQuery<
      SELF extends InferredVarStateQuery<SELF, I, T, L, L_I, L_T, R>,
      I,
      T extends EvalValue<?, ?, I, ?>,
      L extends Lattice<I>,
      L_I extends InferredLattice<I, L>,
      L_T extends EvalLattice<?, I, ?, T, ?, L_I, ?>,
      R extends Result<I, L, ?>>
  extends SimplifiedJavaFlowAnalysisQuery<SELF, R, T, L_T> {
    protected InferredVarStateQuery(
        final IThunk<? extends IJavaFlowAnalysis<T, L_T>> thunk) {
      super(thunk);
    }
    
    protected InferredVarStateQuery(final Delegate<SELF, R, T, L_T> d) {
      super(d);
    }
    
    @Override
    protected final RawResultFactory getRawResultFactory() {
      return RawResultFactory.NORMAL_EXIT;
    }
  }

  

  /**
   * Abstract result type for the the "inferred variables query"
   * {@link InferredVarStateQuery}.
   *
   * @param <I> The type of the state to be inferred for each local variable.
   * @param <L> The lattice type of the inferred variable states.
   */
  public static abstract class Result<I, L extends Lattice<I>, P extends PromiseDrop<?>>
  implements Iterable<InferredVarState<I>> {
    protected final L inferredStateLattice;
    private final IRNode[] keys;
    private final I[] values;
    
    protected Result(final IRNode[] keys, final I[] val, final L sl) {
      this.keys = keys;
      this.values = val;
      this.inferredStateLattice = sl;
    }
    
    @Override
    public final Iterator<InferredVarState<I>> iterator() {
      return new AbstractRemovelessIterator<InferredVarState<I>>() {
        private int idx = 0;
        
        @Override
        public boolean hasNext() {
          return idx < keys.length;
        }

        @Override
        public InferredVarState<I> next() {
          final int currentIdx = idx++;
          final I inferredState = values[currentIdx];
          return new InferredVarState<I>(
              keys[currentIdx], inferredState);
        }
      };
    }
    
    public final boolean lessEq(final I a, final I b) {
      return inferredStateLattice.lessEq(a, b);
    }

    public abstract P getPromiseDrop(IRNode n);
    
    public abstract I injectPromiseDrop(P pd);
  }
  
  
  
  public static final class InferredVarState<I>
  extends Pair<IRNode, I> {
    public InferredVarState(
        final IRNode varDecl, final I state) {
      super(varDecl, state);
    }
    
    public IRNode getLocal() { return first(); }
    public I getState() { return second(); }
  }
}
