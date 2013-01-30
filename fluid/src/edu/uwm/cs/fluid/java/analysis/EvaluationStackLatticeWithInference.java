package edu.uwm.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.util.ImmutableList;
import edu.uwm.cs.fluid.util.AssociativeArrayLattice;
import edu.uwm.cs.fluid.util.Lattice;
import edu.uwm.cs.fluid.util.PairLattice;

/**
 * Specialization of {@link EvaluationStackLattice} where the "state" 
 * component of the value is itself a pair, where  the first component is 
 * arbitrary state, and the second component is an array of values meant 
 * to correspond to inferred state for local variables.
 *
 * <p>Mostly this class is a container of specialized class declarations that
 * care making sure the types of things are correct.
 * 
 * @param <V> The type of the stack elements.
 * @param <S> The type of the arbitrary analysis state.
 * @param <I> The type of the state to be inferred for each local variable
 * @param <SS> The type of overall state object used during evaluation.
 * @param <L1> The lattice type of the stack elements.
 * @param <L2> The lattice type of the arbitrary analysis state.
 * @param <L3> The lattice type of the array of inferred variable states.
 * @param <L4> The lattice type of the overall state object.
 * @param <V> The type of the overall value used by analysis.  
 */
public abstract class EvaluationStackLatticeWithInference<
    V,
    S,
    I,
    SS extends EvaluationStackLatticeWithInference.StatePair<S, I>,
    L1 extends Lattice<V>,
    L2 extends Lattice<S>,
    L3 extends AssociativeArrayLattice<IRNode, ? extends Lattice<I>, I>,
    L4 extends EvaluationStackLatticeWithInference.StatePairLattice<S, I, L2, L3, SS>,
    R extends EvaluationStackLatticeWithInference.EvalValue<V, S, I, SS>>
extends EvaluationStackLattice<V, SS, L1, L4, R> {
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
   * Lattice type for the state component of the evaluation. 
   *
   * @param <S> The type of the arbitrary analysis state.
   * @param <I> The type of the state to be inferred for each local variable.
   * @param <L1> The lattice type for the arbitrary analysis state.
   * @param <L2> The lattice type for the array of inferred variable states.
   * @param <V> The type of overall state object used during evaluation.
   */
  public static abstract class StatePairLattice<
      S, I, L1 extends Lattice<S>,
      L2 extends AssociativeArrayLattice<IRNode, ? extends Lattice<I>, I>,
      V extends StatePair<S, I>>
  extends PairLattice<S, I[], L1, L2, V> {
    protected StatePairLattice(final L1 l1, final L2 l2) {
      super(l1, l2);
    }
    
    
    
    public final L2 getInferredLattice() {
      return lattice2;
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



  protected EvaluationStackLatticeWithInference(final L1 l1, final L4 l2) {
    super(l1, l2);
  }
  
  
  
  public final L3 getInferredLattice() {
    return lattice2.getInferredLattice();
  }
  
  
  
  public final int indexOfInferred(final IRNode var) {
    return lattice2.indexOfInferred(var);
  }
  
  public final R inferVar(final R v, final int idx, final I e) {
    return newPair(v.first(), lattice2.inferVar(v.second(), idx, e));
  }
}
