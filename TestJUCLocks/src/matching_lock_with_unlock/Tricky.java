package matching_lock_with_unlock;


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * TODO: Need to fix the comments for this one.
 */
@com.surelogic.RegionLock("L is lockField protects data")
public class Tricky {
  private final Lock lockField = new ReentrantLock();
  private int data;

  /*
   * recall that we decided that: lock() is guaranteed NOT to lock before
   * throwing a runtime exception unlock() is guaranteed to UNLOCK a lock (if it
   * was locked) before throwing any runtime exception The somewhat strange
   * semantics for unlock was so that we could depend on it actually unlocking
   * things. Otherwise, the analysis has no way to ensure a lock will actually
   * get unlocked.
   * 
   * If you look at tricky1: Inside inner "try" block, suppose the lock() call
   * fails. In this situation, you try to unlock anyway. That's bad. tricky3
   * doesn't have this problem. If the inner unlock fails, it fails cleanly
   * (after unlocking) and execution is done. Otherwise, if the next lock()
   * fails, we are similarly cleanly done.
   */
  public void tricky1(final boolean flag) {
    lockField.lock();  // A: Different # of matching unlocks because of possible failure of inner lock() call
    try {
      if (flag) {
        lockField.unlock();  // B: Matches [A]
        lockField.lock();  // C: Matches [D]
      }
    } finally {
      lockField.unlock();  // D:  Different # of matching locks because of possible failure of inner lock() call
    }
  }
  
  public void tricky3(final boolean flag) {
    lockField.lock();  // E: Matches [F, H]
    if (flag) {
      lockField.unlock();  // F: Matches [E]
      lockField.lock();  // G: Matches [H]
    }
    lockField.unlock();  // H: Matches [E, G]
  }
}
