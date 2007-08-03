package test_requires_lock;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Lock RW is rwLock protects Region
 * @Region private Region
 */
public class CannotRequireRWLock {
  private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
  
  /**
   * BAD: Must require either the read or the write lock
   * @TestResult is UNASSOCIATED: Cannot require a read-write lock, only it's read or write component
   * @RequiresLock RW
   */
  private void bad() {
    // do stuff;
  }
}
