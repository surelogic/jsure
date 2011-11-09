/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/sea/drops/MaybeTopLevel.java,v 1.1 2007/09/11 15:00:23 dfsuther Exp $*/
package edu.cmu.cs.fluid.sea.drops;

/**
 * Drops that sometimes wish to be displayed at the top level should implement
 * this interface.
 * 
 * @author dfsuther
 */
public interface MaybeTopLevel {
  String REQUEST_TOP_LEVEL = "request-top-level";

  /**
   * Allow drop to request that it be placed at the top level.
   * 
   * @return TRUE when drop is requesting top level placement. FALSE when drop
   *         has no special request.
   */
  public boolean requestTopLevel();

}
