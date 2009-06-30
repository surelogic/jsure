/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/ScrubberOrder.java,v 1.1 2007/08/21 16:42:31 chance Exp $*/
package com.surelogic.annotation.scrub;

public enum ScrubberOrder {
  /**
   * Run this scrubber before NORMAL/LAST, and use dependencies
   * to order within this category
   */
  FIRST,
  /**
   * Run this scrubber only based on specified dependencies
   */
  NORMAL,
  /**
   * Run this scrubber after FIRST/NORMAL, and use dependencies
   * to order within this category
   */
  LAST,
}
