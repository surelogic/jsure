package test_returns_lock;

import com.surelogic.PolicyLock;
import com.surelogic.PolicyLocks;
import com.surelogic.ReturnsLock;

/**
 * Tests that static policy locks cannot be referenced through "this" or 
 * parameter names.
 * (These tests are all really Lock Name sanity tests.)
 */
@PolicyLocks({@PolicyLock("IL is this"),
  @PolicyLock("SL is class")})
public class C_Policy {
  /**
   * BAD: instance-qualified static lock
   */
  @ReturnsLock("this:SL" /* is UNASSOCIATED: Cannot qualify static lock with 'this' */)
  public Object getLock() {
    return C_Policy.class;
  }

  /**
   * BAD: instance-qualified static lock
   */
  @ReturnsLock("p:SL" /*is UNASSOCIATED: Cannot qualify static lock with parameter */)
  public Object getLock(final C_Policy p) {
    return C_Policy.class;
  }
  
  /**
   * GOOD: implicitly class-qualified static lock
   */
  @ReturnsLock("SL" /* is CONSISTENT */)
  public Object getLock2() {
    return C_Policy.class;
  }

  /**
   * Good: instance-qualified instance lock
   */
  @ReturnsLock("this:IL" /*is CONSISTENT*/)
  public Object getInstanceLock() {
    return this;
  }

  /**
   * Good: parameter-qualified instance lock
   */
  @ReturnsLock("p:IL" /* is CONSISTENT */)
  public Object getInstanceLock(final C_Policy p) {
    return p;
  }
  

  
  @PolicyLock("InnerLock is this")
  public class Inner1 {
    public class Inner2 {
      /**
       * GOOD: Qualified receiver exists, names instance lock.
       */
      @ReturnsLock("test_returns_lock.C_Policy.this:IL" /* is CONSISTENT */)
      public Object getLock_good() {
        return C_Policy.this;
      }

      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * BUT THE METOD IS BAD: It returns the wrong lock
       */
      @ReturnsLock("test_returns_lock.C_Policy.this:IL" /* is INCONSISTENT*/)
      public Object getLock_bad() {
        return Inner1.this;
      }

      /**
       * GOOD: Qualified receiver exists, names instance lock.
       */
      @ReturnsLock("test_returns_lock.C_Policy.Inner1.this:InnerLock" /* is CONSISTENT */)
      public Object getInnerLock_good() {
        return Inner1.this;
      }
    }
    
    /**
     * GOOD: Qualified receiver exists, names instance lock.
     */
    @ReturnsLock("test_returns_lock.C_Policy.this:IL" /* is CONSISTENT */)
    public Object getLock_good() {
      return C_Policy.this;
    }
    
    /**
     * BAD: qualified receiver doesn't exist
     */
    @ReturnsLock("test.CC.this:SL" /* is UNBOUND: test.CC doesn't exist */)
    public Object getLock_badQualifiedReceiver() {
      return C_Policy.class;
    }

    /**
     * BAD: instance-qualified static lock
     */
    @ReturnsLock("test_returns_lock.C_Policy.this:SL" /* is UNASSOCIATED: instance-qualified static lock */)
    public Object getLock() {
      return C_Policy.class;
    }

    /**
     * GOOD: Class-qualified static lock
     */
    @ReturnsLock("test_returns_lock.C_Policy:SL" /* is CONSISTENT */)
    public Object getLock2() {
      return C_Policy.class;
    }
  }
}
