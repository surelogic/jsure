package test_requires_lock;

import com.surelogic.Lock;
import com.surelogic.Locks;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.RequiresLock;

/**
 * Tests that static state locks cannot be referenced through "this" or 
 * parameter names.
 * (These tests are all really Lock Name sanity tests.)
 */
@Regions({ 
  @Region("I"),
  @Region("static S")
})
@Locks({
  @Lock("IL is this protects I"),
  @Lock("SL is class protects S")
})
public class C {
  /**
   * BAD: instance-qualified static lock
   */
  @RequiresLock("this:SL" /* is UNASSOCIATED: receiver-qualified static lock */)
  public void bad1() {}

  /**
   * BAD: instance-qualified static lock
   */
  @RequiresLock("p:SL" /* is UNASSOCIATED: parameter-qualified static lock */)
  public void bad2(final C p) {}
  
  /**
   * GOOD: implicitly class-qualified static lock
   */
  @RequiresLock("SL" /* is CONSISTENT */)
  public void good1() {}

  /**
   * Good: instance-qualified instance lock
   */
  @RequiresLock("this:IL" /* is CONSISTENT */)
  public void good2() {}

  /**
   * Good: parameter-qualified instance lock
   */
  @RequiresLock("p:IL" /* is CONSISTENT */)
  public void good3(final C p) {}
  

  

  @Lock("InnerLock is this protects Instance")
  public class Inner1 {
    public class Inner2 {
      /**
       * GOOD: Qualified receiver exists, names instance lock.
       */
      @RequiresLock("test_requires_lock.C.this:IL" /* is CONSISTENT */)
      public void good1() {}

      /**
       * GOOD: Qualified receiver exists, names instance lock.
       */
      @RequiresLock("test_requires_lock.C.Inner1.this:InnerLock" /* is CONSISTENT */)
      public void getInnerLock_good() {}
    }
    
    /**
     * GOOD: Qualified receiver exists, names instance lock.
     */
    @RequiresLock("test_requires_lock.C.this:IL" /* is CONSISTENT */)
    public void good1() {}
    
    /**
     * BAD: qualified receiver doesn't exist
     */
    @RequiresLock("test.CC.this:SL" /* is UNBOUND: qualified receiver doesn't exist */)
    public void bad1() {}

    /**
     * BAD: instance-qualified static lock
     */
    @RequiresLock("test_requires_lock.C.this:SL" /* is UNASSOCIATED: instance-qualified static lock */)
    public void bad2() {}

    /**
     * GOOD: Class-qualified static lock
     */
    @RequiresLock("test_requires_lock.C:SL" /* is CONSISTENT */)
    public void good2() {}
  }
}
