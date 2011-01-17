package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Abstraction of a query to an analysis. Implementations of
 * {@link edu.uwm.cs.fluid.java.analysis.IntraproceduralAnalysis} and other
 * analysis engines, such as {@link Effects} have getter methods that return
 * instances of these. This class is primarily intended to abstract away the
 * constructor context parameter of the {@code getAnalysisResults} methods and
 * whether the initializer block is being analyzed or not. They are also useful
 * for hiding any result manipulation that needs to done by passing a raw
 * analysis result through a method in the analysis lattice, for example the
 * method
 * {@link com.surelogic.analysis.bca.BindingContext#expressionObjects(edu.cmu.cs.fluid.util.ImmutableSet[], IRNode)}
 * in the case of binding context analysis.
 * 
 * @param <R>
 *          The type of the analysis result.
 */
public interface AnalysisQuery<R> {
  /**
   * Get the analysis result for the given node.
   */
  public R getResultFor(IRNode expr);
}
