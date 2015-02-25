package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IDrop;

/**
 * An interface that can filter drops based upon a criteria an implementing
 * implementation selects.
 */
public interface IDropFilter {
  /**
   * Judgment if a particular drop should be kept.
   * 
   * @param d
   *          a drop.
   * @return {@code true} if the drop should be kept, {@code false} if it should
   *         not be filtered out.
   */
  boolean keep(IDrop d);
}
