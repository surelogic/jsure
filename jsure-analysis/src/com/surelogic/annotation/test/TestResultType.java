/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/test/TestResultType.java,v 1.2 2007/07/20 16:46:11 chance Exp $*/
package com.surelogic.annotation.test;

public enum TestResultType {
  /**
   * Promise resulted in parse error
   */
  UNPARSEABLE(411), 
  /**
   * Promise parsed OK
   */
  PARSED(1),
  /**
   * Promise resulted in an AAST, but had unbound elements
   * -- use the context as the type of the first unbound element
   */
  UNBOUND(511), 
  /**
   * Promise resulted in an bound AAST
   * Used for status so far
   */
  BOUND(2),
  /**
   * Promise resulted in an bound AAST, but was rejected by a scrubber
   * for some other reason, and so unassociated with a PromiseDrop
   * -- use the context as a messageId 
   */
  UNASSOCIATED(911),
  /**
   * Promise resulted in a valid PromiseDrop with AST
   */
  VALID(3),
  /**
   * Promise resulted in a valid PromiseDrop that was deemed 
   * inconsistent with the code
   */
  INCONSISTENT(4),
  /**
   * Promise resulted in a valid PromiseDrop that was deemed 
   * consistent with the code
   */
  CONSISTENT(5);
  
  public final int value;
  
  private TestResultType(int passed) {
    value = passed;
  }
}
