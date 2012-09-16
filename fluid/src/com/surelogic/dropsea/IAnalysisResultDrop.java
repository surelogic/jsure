package com.surelogic.dropsea;

import java.util.Collection;

/**
 * The interface for the base class for all analysis results, or verification
 * judgement, drops within the sea, intended to allow multiple implementations.
 * <p>
 * The verifying analyses use the IR drop-sea and the Eclipse client loads
 * snapshots using the IR-free drop-sea.
 */
public interface IAnalysisResultDrop extends IProofDrop, IAnalysisOutputDrop {
  /**
   * Gets the set of promise drops established, or checked, by this result.
   * 
   * @return the non-null (possibly empty) set of promise drops established, or
   *         checked, by this result.
   */
  Collection<? extends IPromiseDrop> getChecks();
}
