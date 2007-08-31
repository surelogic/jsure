package test_returns_lock;

/**
 * Tests that static policy locks cannot be referenced through "this" or 
 * parameter names.
 * (These tests are all really Lock Name sanity tests.)
 * 
 * @PolicyLock IL is this
 * @PolicyLock SL is class
 *
 */
public class C_Policy {
  /**
   * BAD: instance-qualified static lock
   * @TestResult is UNASSOCIATED: Cannot qualify static lock with 'this'
   * @ReturnsLock this:SL
   */
  public Object getLock() {
    return C_Policy.class;
  }

  /**
   * BAD: instance-qualified static lock
   * @TestResult is UNASSOCIATED: Cannot qualify static lock with parameter
   * @ReturnsLock p:SL
   */
  public Object getLock(final C_Policy p) {
    return C_Policy.class;
  }

  /**
   * GOOD: implicitly class-qualified static lock
   * @TestResult is CONSISTENT
   * @ReturnsLock SL
   */
  public Object getLock2() {
    return C_Policy.class;
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
  public Object getInstanceLock(final C_Policy p) {
    return p;
  }
  

  
  /**
   * @PolicyLock InnerLock is this
   */
  public class Inner1 {
    public class Inner2 {
      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * @TestResult is CONSISTENT
       * @ReturnsLock test_returns_lock.C_Policy.this:IL
       */
      public Object getLock_good() {
        return C_Policy.this;
      }

      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * BUT THE METHOD IS BAD: It returns the wrong lock
       * @TestResult is INCONSISTENT: Returns the wrong policy lock
       * @ReturnsLock test_returns_lock.C_Policy.this:IL
       */
      public Object getLock_bad() {
        return Inner1.this;
      }

      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * @TestResult is CONSISTENT
       * @ReturnsLock test_returns_lock.C_Policy.Inner1.this:InnerLock
       */
      public Object getInnerLock_good() {
        return Inner1.this;
      }
    }
    
    /**
     * GOOD: Qualified receiver exists, names instance lock.
     * @TestResult is CONSISTENT
     * @ReturnsLock test_returns_lock.C_Policy.this:IL
     */
    public Object getLock_good() {
      return C_Policy.this;
    }
    
    /**
     * BAD: qualified receiver doesn't exist
     * @TestResult is UNBOUND: test.CC doesn't exist
     * @ReturnsLock test.CC.this:SL
     */
    public Object getLock_badQualifiedReceiver() {
      return C_Policy.class;
    }

    /**
     * BAD: instance-qualified static lock
     * @TestResult is UNASSOCIATED: instance-qualified static lock
     * @ReturnsLock test_returns_lock.C_Policy.this:SL
     */
    public Object getLock() {
      return C_Policy.class;
    }

    /**
     * GOOD: Class-qualified static lock
     * @TestResult is CONSISTENT
     * @ReturnsLock test_returns_lock.C_Policy:SL
     */
    public Object getLock2() {
      return C_Policy.class;
    }
  }
}
