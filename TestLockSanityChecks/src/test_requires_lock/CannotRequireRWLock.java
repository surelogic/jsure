package test_requires_lock;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.RegionLock;
import com.surelogic.Region;
import com.surelogic.RequiresLock;

@Region("private Region")
@RegionLock("RW is rwLock protects Region")
public class CannotRequireRWLock {
  private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
  
  /**
   * BAD: Must require either the read or the write lock
   */
  @RequiresLock("RW" /* is UNASSOCIATED: Cannot require a read-write lock, only it's read or write component */)
  private void bad() {
    // do stuff;
  }
}
