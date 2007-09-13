package test_returns_lock;

/**
 * Tests that static state locks cannot be referenced through "this" or 
 * parameter names.
 * (These tests are all really Lock Name sanity tests.)
 * 
 * @TestResult is CONSISTENT
 * @Region I
 * @TestResult is CONSISTENT
 * @Region static S
 * 
 * @RegionLock IL is this protects I
 * @RegionLock SL is class protects S
 *
 */
public class C {
  /**
   * BAD: instance-qualified static lock
   * @TestResult is UNASSOCIATED : Cannot qualify a static lock with this
   * @ReturnsLock this:SL
   */
  public Object getLock() {
    return C.class;
  }

  /**
   * BAD: instance-qualified static lock
   * @TestResult is UNASSOCIATED : Cannot qualify a static lock with a parameter
   * @ReturnsLock p:SL
   */
  public Object getLock(final C p) {
    return C.class;
  }
  
  /**
   * GOOD: implicitly class-qualified static lock
   * @TestResult is CONSISTENT
   * @ReturnsLock SL
   */
  public Object getLock2() {
    return C.class;
  }

  /**
   * Good: instance-qualified instance lock
   * @TestResult is CONSISTENT
   * @ReturnsLock this:IL
   */
  public Object getInstanceLock() {
    return this;
  }

  /**
   * Good: parameter-qualified instance lock
   * @TestResult is CONSISTENT
   * @ReturnsLock p:IL
   */
  public Object getInstanceLock(final C p) {
    return p;
  }
  

  
  /**
   * @RegionLock InnerLock is this protects Instance
   */
  public class Inner1 {    
    public class Inner2 {
      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * @TestResult is CONSISTENT
       * @ReturnsLock test_returns_lock.C.this:IL
       */
      public Object getLock_good() {
        return C.this;
      }

      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * BUT THE METHOD IS BAD: It returns the wrong lock
       * @TestResult is INCONSISTENT: Returns the wrong state lock
       * @ReturnsLock test_returns_lock.C.this:IL
       */
      public Object getLock_bad() {
        return Inner1.this;
      }

      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * @TestResult is CONSISTENT
       * @ReturnsLock test_returns_lock.C.Inner1.this:InnerLock
       */
      public Object getInnerLock_good() {
        return Inner1.this;
      }
    }
    
    /**
     * GOOD: Qualified receiver exists, names instance lock.
     * @TestResult is CONSISTENT
     * @ReturnsLock test_returns_lock.C.this:IL
     */
    public Object getLock_good() {
      return C.this;
    }
    
    /**
     * BAD: qualified receiver doesn't exist
     * @TestResult is UNBOUND: test.CC doesn't exist
     * @ReturnsLock test.CC.this:SL
     */
    public Object getLock_badQualifiedReceiver() {
      return C.class;
    }

    /**
     * BAD: instance-qualified static lock
     * @TestResult is UNASSOCIATED: instance-qualified static lock
     * @ReturnsLock test_returns_lock.C.this:SL
     */
    public Object getLock() {
      return C.class;
    }

    /**
     * GOOD: Class-qualified static lock
     * @TestResult is CONSISTENT
     * @ReturnsLock test_returns_lock.C:SL
     */
    public Object getLock2() {
      return C.class;
    }
  }
}
