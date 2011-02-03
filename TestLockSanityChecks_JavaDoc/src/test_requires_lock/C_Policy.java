package test_requires_lock;

/**
 * Tests that static policy locks cannot be referenced through "this" or 
 * parameter names.
 * (These tests are all really Lock Name sanity tests.)
 * 
 * 
 * 
 * 
 * @PolicyLock IL is this
 * @PolicyLock SL is class
 *
 */
public class C_Policy {
  /**
   * BAD: instance-qualified static lock
   * @TestResult is UNASSOCIATED : receiver-qualified static lock
   * @requiresLock this:SL
   */
  public void bad1() {}

  /**
   * BAD: instance-qualified static lock
   * @TestResult is UNASSOCIATED : parameter-qualified static lock
   * @RequiresLock p:SL
   */
  public void bad2(final C_Policy p) {}
  
  /**
   * GOOD: implicitly class-qualified static lock
   * @TestResult is CONSISTENT
   * @requiresLock SL
   */
  public void good1() {}

  /**
   * Good: receiver-qualified instance lock
   * @TestResult is CONSISTENT 
   * @requiresLock this:IL
   */
  public void good2() {}

  /**
   * Good: parameter-qualified instance lock
   * @TestResult is CONSISTENT
   * @requiresLock p:IL
   */
  public void good3(final C_Policy p) {}
  

  

  /**
   * @PolicyLock InnerLock is this
   */
  public class Inner1 {
    public class Inner2 {
      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * @TestResult is CONSISTENT
       * @RequiresLock test_requires_lock.C_Policy.this:IL
       */
      public void good1() {}

      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * @TestResult is CONSISTENT
       * @RequiresLock test_requires_lock.C_Policy.Inner1.this:InnerLock
       */
      public void getInnerLock_good() {}
    }
    
    /**
     * GOOD: Qualified receiver exists, names instance lock.
     * @TestResult is CONSISTENT
     * @RequiresLock test_requires_lock.C_Policy.this:IL
     */
    public void good1() {}
    
    /**
     * BAD: qualified receiver doesn't exist
     * @TestResult is UNBOUND: test.CC_Policy doesn't exist
     * @RequiresLock test.CC_Policy.this:SL
     */
    public void bad1() {}

    /**
     * BAD: instance-qualified static lock
     * @TestResult is UNASSOCIATED: instance-qualified static lock
     * @RequiresLock test_requires_lock.C_Policy.this:SL
     */
    public void bad2() {}

    /**
     * GOOD: Class-qualified static lock
     * @TestResult is CONSISTENT
     * @RequiresLock test_requires_lock.C_Policy:SL
     */
    public void good2() {}
  }
}
