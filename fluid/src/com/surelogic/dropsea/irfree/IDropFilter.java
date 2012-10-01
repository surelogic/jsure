package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IDrop;

public interface IDropFilter {
  /**
   * Judgment if a particular drop should be shown.
   * 
   * @param d
   *          a drop.
   * @return {@code true} if the drop should be shown, {@code false} otherwise.
   */
  boolean show(IDrop d);

  static final IDropFilter nullFilter = new IDropFilter() {
    public boolean show(IDrop d) {
      return true;
    }
  };
}
