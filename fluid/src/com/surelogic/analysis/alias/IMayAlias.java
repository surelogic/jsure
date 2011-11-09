package com.surelogic.analysis.alias;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Interface for simple alias analyses that answer the "may alias" question
 * and do not care about the point in the code where the question is being
 * asked.
 */
public interface IMayAlias {
  /**
   * Is it possible that the two expressions may refer to the same object?
   */
  public boolean mayAlias(IRNode expr1, IRNode expr2);
}
