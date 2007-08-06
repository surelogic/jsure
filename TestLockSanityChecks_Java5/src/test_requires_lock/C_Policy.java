package test_requires_lock;

import com.surelogic.PolicyLock;
import com.surelogic.PolicyLocks;
import com.surelogic.RequiresLock;

/**
 * Tests that static policy locks cannot be referenced through "this" or 
 * parameter names.
 * (These tests are all really Lock Name sanity tests.)
 */
@PolicyLocks({
  @PolicyLock("IL is this"),
  @PolicyLock("SL is class")
})
public class C_Policy {
  /**
   * BAD: instance-qualified static lock
   */
  @RequiresLock("this:SL" /*is UNASSOCIATED : receiver-qualified static lock*/)
  public void bad1() {}

  /**
   * BAD: instance-qualified static lock
   */
  @RequiresLock("p:SL" /*is UNASSOCIATED : parameter-qualified static lock*/)
  public void bad2(final C_Policy p) {}
  
  /**
   * GOOD: implicitly class-qualified static lock
   */
  @RequiresLock("SL" /*is CONSISTENT*/)
  public void good1() {}

  /**
   * Good: receiver-qualified instance lock
   */
  @RequiresLock("this:IL" /*is CONSISTENT*/)
  public void good2() {}

  /**
   * Good: parameter-qualified instance lock
   */
  @RequiresLock("p:IL" /*is CONSISTENT*/)
  public void good3(final C_Policy p) {}
  

  
  @PolicyLock("InnerLock is this")
  public class Inner1 {
    public class Inner2 {
      /**
       * GOOD: Qualified receiver exists, names instance lock.
       */
      @RequiresLock("test_requires_lock.C_Policy.this:IL" /* is CONSISTENT */)
      public void good1() {}

      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * @returnsLock test_requires_lock.C_Policy.Inner1.this:InnerLock
       */
      @RequiresLock("test_requires_lock.C_Policy.Inner1.this:InnerLock" /* is CONSISTENT */)
      public void getInnerLock_good() {}
    }
    
    /**
     * GOOD: Qualified receiver exists, names instance lock.
     */
    @RequiresLock("test_requires_lock.C_Policy.this:IL" /* is CONSISTENT */)
    public void good1() {}
    
    /**
     * BAD: qualified receiver doesn't exist
     */
    @RequiresLock("test.CC_Policy.this:SL" /* is UNBOUND: test.CC_Policy doesn't exist */)
    public void bad1() {}

    /**
     * BAD: instance-qualified static lock
     */
    @RequiresLock("test_requires_lock.C_Policy.this:SL" /* is UNASSOCIATED: instance-qualified static lock */)
    public void bad2() {}

    /**
     * GOOD: Class-qualified static lock
     */
    @RequiresLock("test_requires_lock.C_Policy:SL" /* is CONSISTENT */)
    public void good2() {}
  }
}
