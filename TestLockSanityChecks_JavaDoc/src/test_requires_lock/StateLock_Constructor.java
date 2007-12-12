package test_requires_lock;

/**
 * Test that constructors cannot require locks on the instance being
 * constructed.  Static locks on the class can be used.  Instance locks
 * on parameters can be required.
 * 
 *
 * @Region static StaticRegion
 * @Region NonStaticRegion
 * @RegionLock StaticLock is class protects StaticRegion
 * @RegionLock NonStaticLock is this protects NonStaticRegion
 */
public class StateLock_Constructor {
  /**
   * BAD: Constructor requires (implicit) instance lock on "this"
   * @TestResult is UNASSOCIATED: Constructor requires (implicit) instance lock on "this"
   * @RequiresLock NonStaticLock
   */
  public StateLock_Constructor(int x) {}
  
  /**
   * BAD: Constructor requires instance lock on "this"
   * @TestResult is UNASSOCIATED: Constructor requires instance lock on "this"
   * @RequiresLock this:NonStaticLock
   */
  public StateLock_Constructor(int x, int y) {}
  
  /**
   * GOOD: Constructor requires instance lock on a parameter
   * @TestResult is CONSISTENT
   * @RequiresLock p:NonStaticLock
   */
  public StateLock_Constructor(final StateLock_Constructor p) {}
  
  /**
   * GOOD: Constructor requires static lock of class
   * @TestResult is CONSISTENT
   * @RequiresLock StaticLock
   */
  public StateLock_Constructor(int x, int y, int z) {}
  
  /**
   * GOOD: Constructor requires static lock of class
   * @TestResult is CONSISTENT
   * @RequiresLock test_requires_lock.StateLock_Constructor:StaticLock
   */
  public StateLock_Constructor(int x, int y, int z, int w) {}
}
