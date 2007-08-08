package test_requires_lock;

/**
 * Test that constructors cannot require locks on the instance being
 * constructed.  Static locks on the class can be used.  Instance locks
 * on parameters can be required.
 * 
 *
 *
 * @PolicyLock StaticLock is class
 * @PolicyLock NonStaticLock is this 
 */
public class PolicyLock_Constructor {
  /**
   * BAD: Constructor requires (implicit) instance lock on "this"
   * @TestResult is UNASSOCIATED: Constructor requires (implicit) instance lock on "this"
   * @RequiresLock NonStaticLock
   */
  public PolicyLock_Constructor(int x) {}
  
  /**
   * BAD: Constructor requires instance lock on "this"
   * @TestResult is UNASSOCIATED: Constructor requires instance lock on "this"
   * @RequiresLock this:NonStaticLock
   */
  public PolicyLock_Constructor(int x, int y) {}
  
  /**
   * GOOD: Constructor requires instance lock on a parameter
   * @TestResult is CONSISTENT
   * @RequiresLock p:NonStaticLock
   */
  public PolicyLock_Constructor(final PolicyLock_Constructor p) {}
  
  /**
   * GOOD: Constructor requires static lock of class
   * @TestResult is CONSISTENT
   * @RequiresLock StaticLock
   */
  public PolicyLock_Constructor(int x, int y, int z) {}
  
  /**
   * GOOD: Constructor requires static lock of class
   * @TestResult is CONSISTENT
   * @RequiresLock test_requires_lock.PolicyLock_Constructor:StaticLock
   */
  public PolicyLock_Constructor(int x, int y, int z, int w) {}
}
