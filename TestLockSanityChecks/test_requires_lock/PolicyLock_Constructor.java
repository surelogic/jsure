package test_requires_lock;

/**
 * Test that constructors cannot require locks on the instance being
 * constructed.  Static locks on the class can be used.  Instance locks
 * on parameters can be required.
 * 
 *
 *
 * @policyLock StaticLock is class
 * @policyLock NonStaticLock is this 
 */
public class PolicyLock_Constructor {
  /**
   * BAD: Constructor requires (implicit) instance lock on "this"
   * @requiresLock NonStaticLock
   */
  public PolicyLock_Constructor(int x) {}
  
  /**
   * BAD: Constructor requires instance lock on "this"
   * @requiresLock this.NonStaticLock
   */
  public PolicyLock_Constructor(int x, int y) {}
  
  /**
   * GOOD: Constructor requires instance lock on a parameter
   * @requiresLock p.NonStaticLock
   */
  public PolicyLock_Constructor(final PolicyLock_Constructor p) {}
  
  /**
   * GOOD: Constructor requires static lock of class
   * @requiresLock StaticLock
   */
  public PolicyLock_Constructor(int x, int y, int z) {}
  
  /**
   * GOOD: Constructor requires static lock of class
   * @requiresLock test_requires_lock.PolicyLock_Constructor:StaticLock
   */
  public PolicyLock_Constructor(int x, int y, int z, int w) {}
}
