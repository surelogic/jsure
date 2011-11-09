/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/AASTStatus.java,v 1.4 2007/07/20 16:46:11 chance Exp $*/
package com.surelogic.aast;

import com.surelogic.annotation.test.TestResultType;

public enum AASTStatus {
  UNPROCESSED(0),
  /**
   * Bindings look good
   */
  BOUND(TestResultType.BOUND.value),
  /**
   * Attached to a drop
   */
  VALID(TestResultType.VALID.value),
  /**
   * Bindings look bad
   */
  UNBOUND(TestResultType.UNBOUND.value),
  /**
   * Unattached to a drop
   */
  UNASSOCIATED(TestResultType.UNASSOCIATED.value);
  
  /**
   * Represents how far the AAST should go through processing
   */
  public final int value;
  
  private AASTStatus(int passed) {
    value = passed;
  }
}
