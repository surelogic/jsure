package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Analysis query interface for queries based on control flow analysis. The
 * query is further specialized to deal with the subsidiary flow analysis
 * objects that are necessary for handling instance initializer blocks when
 * analyzing {@code super()} constructor calls and anonymous class expressions.
 */
public interface HasSubQuery {
  /**
   * Get the query that is based on the initializer block subanalysis associated
   * with the analysis used by this query. Implementations should specialize the
   * return type of this method to match the type of implementation.
   * 
   * @param caller
   *          The ConstructorCall or NewExpression node associated with the
   *          initializer block. If a NewExpression node, it must be a child of
   *          an AnonClassExpression node.
   * @return A new query for the given initializer block. This is never {@code
   *         null}. If the query doesn't exist an exception is thrown.
   * @exception UnsupportedOperationException
   *              Thrown if a subsidiary flow analysis for the given call does
   *              not exist.
   */
  public HasSubQuery getSubAnalysisQuery(IRNode caller);
}
