package test_requires_lock;

/**
 * Tests that static state locks cannot be referenced through "this" or 
 * parameter names.
 * (These tests are all really Lock Name sanity tests.)
 * 
 * @Region I
 * @Region static S
 * 
 * @RegionLock IL is this protects I
 * @RegionLock SL is class protects S
 *
 */
public class C {
  /**
   * BAD: instance-qualified static lock
   * @TestResult is UNASSOCIATED: receiver-qualified static lock
   * @RequiresLock this:SL
   */
  public void bad1() {}

  /**
   * BAD: instance-qualified static lock
   * @TestResult is UNASSOCIATED: parameter-qualified static lock
   * @RequiresLock p:SL
   */
  public void bad2(final C p) {}
  
  /**
   * GOOD: implicitly class-qualified static lock
   * @TestResult is CONSISTENT
   * @RequiresLock SL
   */
  public void good1() {}

  /**
   * Good: instance-qualified instance lock
   * @TestResult is CONSISTENT
   * @RequiresLock this:IL
   */
  public void good2() {}

  /**
   * Good: parameter-qualified instance lock
   * @TestResult is CONSISTENT
   * @RequiresLock p:IL
   */
  public void good3(final C p) {}
  

  

  /**
   * @RegionLock InnerLock is this protects Instance
   */
  public class Inner1 {
    public class Inner2 {
      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * @TestResult is CONSISTENT
       * @RequiresLock test_requires_lock.C.this:IL
       */
      public void good1() {}

      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * @TestResult is CONSISTENT
       * @RequiresLock test_requires_lock.C.Inner1.this:InnerLock
       */
      public void getInnerLock_good() {}
    }
    
    /**
     * GOOD: Qualified receiver exists, names instance lock.
     * @TestResult is CONSISTENT
     * @RequiresLock test_requires_lock.C.this:IL
     */
    public void good1() {}
    
    /**
     * BAD: qualified receiver doesn't exist
     * @TestResult is UNBOUND: qualified receiver doesn't exist
     * @RequiresLock test.CC.this:SL
     */
    public void bad1() {}

    /**
     * BAD: instance-qualified static lock
     * @TestResult is UNASSOCIATED: instance-qualified static lock
     * @RequiresLock test_requires_lock.C.this:SL
     */
    public void bad2() {}

    /**
     * GOOD: Class-qualified static lock
     * @TestResult is CONSISTENT
     * @RequiresLock test_requires_lock.C:SL
     */
    public void good2() {}
  }
}
