package test_final_exprs;
import com.surelogic.PolicyLock;
import com.surelogic.PolicyLocks;
import com.surelogic.ReturnsLock;



/**
 * Test method calls as final expressions.  Method calls are final if
 * (1) The method is static or the receiver expression is finanl
 * (2) The method has a @returnsLock annotation
 */
@PolicyLocks({
  @PolicyLock("PL is class"),
  @PolicyLock("LL is this")
})
public class TestMethodCall {
  
  /**
   * A static method that has no returnsLock annotation.
   */
  public Object staticMethod_noReturnsLock() {
    return new Object();
  }
  
  /**
   * A static method with a returnsLock annotation
   */
  @ReturnsLock("PL")
  public Object staticMethod_returnsLock() {
    // GOOD: Returns correct lock
    return TestMethodCall.class;
  }
  
  /**
   * An instance method with out a returnsLock annotation
   */
  public Object instanceMethod_noReturnsLock() {
    return null;
  }
  
  /**
   * An instance method with a returnsLock annotation.
   */
  @ReturnsLock("LL")
  public Object instanceMethod_returnsLock() {
    // GOOD: Returns correct lock
    return this;
  }
  
  
  
  public void bad_calls_staticMethod_noReturnsLock() {
    /* NON-FINAL: method is static, but has no returns lock */
    synchronized(staticMethod_noReturnsLock()) {
      // do stuff here
    }
  }

  public void good_calls_staticMethod_returnsLock() {
    /* FINAL: method is static, has returns lock */
    // warning: sync block doesn't protect any state
    synchronized(staticMethod_returnsLock()) {
      // do stuff here
    }
  }
  
  
  
  public void bad_calls_instanceMethod_nonFinalRcvr_noReturnsLock() {
    /* NON-FINAL: method is instance, the receiver expression is non-final,
     * and the method has no returnsLock annotation.
     */
    TestMethodCall t = new TestMethodCall();
    synchronized(t.instanceMethod_noReturnsLock()) {
      t = new TestMethodCall();
      // do stuff here
    }
  }

  public void bad_calls_instanceMethod_nonFinalRcvr_returnsLock() {
    /* NON-FINAL: method is instance, the receiver expression is non-final,
     * although the method has a returnsLock annotation.
     */
    TestMethodCall t = new TestMethodCall();
    synchronized(t.instanceMethod_returnsLock()) {
      t = new TestMethodCall();
      // do stuff here
    }
  }
  
  
  
  public void bad_calls_instanceMethod_finalRcvr_noReturnsLock() {
    /* NON-FINAL: method is instance, the receiver expression is final,
     * but the method has no returnsLock annotation.
     */
    final TestMethodCall t = new TestMethodCall();
    synchronized(t.instanceMethod_noReturnsLock()) {
      // do stuff here
    }
  }

  public void good_calls_instanceMethod_finalRcvr_returnsLock() {
    /* FINAL: method is instance, the receiver expression is final,
     * the method has a returnsLock annotation.
     */
    final TestMethodCall t = new TestMethodCall();
    // warning: sync block doesn't protect any state
    synchronized(t.instanceMethod_returnsLock()) {
      // do stuff here
    }
  }
}
