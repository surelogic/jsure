package test_returns_lock;

import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.ReturnsLock;

/**
 * Tests that static state locks cannot be referenced through "this" or 
 * parameter names.
 * (These tests are all really Lock Name sanity tests.)
 */
@Regions({@Region("I"),
  @Region("static S")})
@RegionLocks({@RegionLock("IL is this protects I"),
  @RegionLock("SL is class protects S")})
public class C {
  /**
   * BAD: instance-qualified static lock
   */
  @ReturnsLock("this:SL" /*is UNASSOCIATED : Cannot qualify a static lock with this*/)
  public Object getLock() {
    return C.class;
  }

  /**
   * BAD: instance-qualified static lock
   */
  @ReturnsLock("p:SL" /*is UNASSOCIATED : Cannot qualify a static lock with a parameter*/)
  public Object getLock(final C p) {
    return C.class;
  }
  
  /**
   * GOOD: implicitly class-qualified static lock
   */
  @ReturnsLock("SL" /*is CONSISTENT*/)
  public Object getLock2() {
    return C.class;
  }

  /**
   * Good: instance-qualified instance lock
   */
  @ReturnsLock("this:IL" /* is CONSISTENT */)
  public Object getInstanceLock() {
    return this;
  }
  
  /**
   * Good: parameter-qualified instance lock
   */
  @ReturnsLock("p:IL" /* is CONSISTENT */)
  public Object getInstanceLock(final C p) {
    return p;
  }
  

  
  @RegionLock("InnerLock is this protects Instance")
  public class Inner1 {
    public class Inner2 {
      /**
       * GOOD: Qualified receiver exists, names instance lock.
       */
      @ReturnsLock("test_returns_lock.C.this:IL" /*is CONSISTENT*/)
      public Object getLock_good() {
        return C.this;
      }

      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * BUT THE METOD IS BAD: It returns the wrong lock
       */
      @ReturnsLock("test_returns_lock.C.this:IL" /*is INCONSISTENT: returns wrong lock */)
      public Object getLock_bad() {
        return Inner1.this;
      }

      /**
       * GOOD: Qualified receiver exists, names instance lock.
       */
      @ReturnsLock("test_returns_lock.C.Inner1.this:InnerLock" /* is CONSISTENT */)
      public Object getInnerLock_good() {
        return Inner1.this;
      }
    }
    
    /**
     * GOOD: Qualified receiver exists, names instance lock.
     */
    @ReturnsLock("test_returns_lock.C.this:IL" /* is CONSISTENT */)
    public Object getLock_good() {
      return C.this;
    }
    
    /**
     * BAD: qualified receiver doesn't exist
     */
    @ReturnsLock("test.CC.this:SL" /* is UNBOUND: test.CC doesn't exist */)
    public Object getLock_badQualifiedReceiver() {
      return C.class;
    }

    /**
     * BAD: instance-qualified static lock
     */
    @ReturnsLock("test_returns_lock.C.this:SL" /* is UNASSOCIATED: instance-qualified static lock */)
    public Object getLock() {
      return C.class;
    }

    /**
     * GOOD: Class-qualified static lock
     */
    @ReturnsLock("test_returns_lock.C:SL" /* is CONSISTENT */)
    public Object getLock2() {
      return C.class;
    }
  }
}
