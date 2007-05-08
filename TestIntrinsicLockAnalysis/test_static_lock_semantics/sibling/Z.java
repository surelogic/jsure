package test_static_lock_semantics.sibling;

/**
 * Uses X.lock as a lock. Point is that we should find that for o.lock, when o
 * is statically of type Z, the lock implementation is the lock declared here,
 * but not the lock declared in Y.
 * 
 * @lock ZLock is lock protects baz
 */
public class Z extends X {
  private int baz;
  
  public int good() {
    synchronized (lock) {
      // GOOD: supporting evidence should not show YLock
      return baz;
    }
  }
}
