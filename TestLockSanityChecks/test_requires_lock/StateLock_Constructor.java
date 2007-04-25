package test_requires_lock;

/**
 * Test that constructors cannot require locks on the instance being
 * constructed.  Static locks on the class can be used.  Instance locks
 * on parameters can be required.
 * 
 * @region static StaticRegion
 * @region NonStaticRegion
 * @lock StaticLock is class protects StaticRegion
 * @lock NonStaticLock is this protects NonStaticRegion
 */
public class StateLock_Constructor {
  /**
   * BAD: Constructor requires (implicit) instance lock on "this"
   * @requiresLock NonStaticLock
   */
  public StateLock_Constructor(int x) {}
  
  /**
   * BAD: Constructor requires instance lock on "this"
   * @requiresLock this.NonStaticLock
   */
  public StateLock_Constructor(int x, int y) {}
  
  /**
   * GOOD: Constructor requires instance lock on a parameter
   * @requiresLock p.NonStaticLock
   */
  public StateLock_Constructor(final StateLock_Constructor p) {}
  
  /**
   * GOOD: Constructor requires static lock of class
   * @requiresLock StaticLock
   */
  public StateLock_Constructor(int x, int y, int z) {}
  
  /**
   * GOOD: Constructor requires static lock of class
   * @requiresLock test_requires_lock.StateLock_Constructor:StaticLock
   */
  public StateLock_Constructor(int x, int y, int z, int w) {}
}
