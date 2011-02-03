/*
 * Created on Sep 22, 2004
 *
 */
package test_policyLock_requiresLock;

/**
 * Test the policy locks can be used with returnsLock and requiresLock.
 * 
 * @PolicyLock PLock is plock
 */
public class Test {
  final Object plock = new Object();
	


  /**
   * Return the policy lock
   * @ReturnsLock PLock
   */
  public Object getPolicyLock() {
    return plock;
  }
  
  /**
   * Requires the policy lock.
   * @RequiresLock PLock
   */
  public void needsPolicyLock() {
    // do stuff
  }

  public void bad_calls_needsPolicyLock() {
    // WARNING: Lock not needed (because it is the WRONG lock here)
    synchronized (this) {
      // BAD: need to lock on plock
      needsPolicyLock();
    }
  }

  public void good_calls_needsPolicyLock() {
    synchronized (plock) {
      // GOOD: plock held
      needsPolicyLock();
    }
  }

  public void good_calls_needsPolicyLock_using_getter() {
    synchronized (getPolicyLock()) {
      // GOOD: plock held
      needsPolicyLock();
    }
  }

  public void bad_calls_needsPolicyLock2(final Test t) {
    // WRONG: should be "t.plock"
    synchronized (plock) {
      // BAD: needs t.plock
      t.needsPolicyLock();
    }
  }

  public void good_calls_needsPolicyLock2(final Test t) {
    synchronized (t.plock) {
      // GOOD: has t.plock
      t.needsPolicyLock();
    }
  }

  public void good_calls_needsPolicyLock2_using_getter(final Test t) {
    synchronized (t.getPolicyLock()) {
      // GOOD: has t.plock
      t.needsPolicyLock();
    }
  }
}
