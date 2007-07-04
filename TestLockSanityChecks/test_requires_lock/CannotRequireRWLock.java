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
   * @RequiresLock RW
   */
  private void bad() {
    // do stuff;
  }
}
