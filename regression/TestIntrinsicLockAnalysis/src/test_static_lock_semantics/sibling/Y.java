package test_static_lock_semantics.sibling;

import com.surelogic.RegionLock;

/**
 * Uses X.lock as a lock. Point is that we should find that for o.lock, when o
 * is statically of type Y, the lock implementation is the lock declared here,
 * but not the lock declared in Z.
 */
@RegionLock("YLock is lock protects bar")
public class Y extends X {
  private int bar;
  
  public int good() {
    synchronized (lock) {
      // GOOD: supporting evidence should not show ZLock
      return bar;
    }
  }
}
