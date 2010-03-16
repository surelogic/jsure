package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Abstraction of a query to an analysis. Implementations of
 * {@link IntraproceduralAnalysis} have getter methods that return instances of
 * these. This class is primarily intended to abstract away the constructor
 * context parameter and whether the initializer block is being analyzed or not.
 * 
 * @param <R>
 *          The type of the analysis result.
 */
public interface AnalysisQuery<R> {
  /**
   * Get the analysis result for the given node.
   */
  public R getResultFor(IRNode expr);

  /**
   * Can the query provide a query based on the initializer block subanalysis
   * that may be associated with the analysis being used by this query.
   * 
   * @param caller
   *          The ConstructorCall or NewExpression node associated with the
   *          initializer block. If a NewExpression node, it must be a child of
   *          an AnonClassExpression node.
   */
  public boolean hasSubAnalysisQuery(IRNode caller);

  /**
   * Get the query that is based on the initializer block subanalysis associated
   * with the analysis used by this query.
   * 
   * @param caller
   *          The ConstructorCall or NewExpression node associated with the
   *          initializer block. If a NewExpression node, it must be a child of
   *          an AnonClassExpression node.
   * @exception UnsupportedOperationException
   *              Thrown if the query does not have a subanalysis query because
   *              the analysis does not have a subanalysis.
   */
  public AnalysisQuery<R> getSubAnalysisQuery(IRNode caller);
}
