package instanceLock.dualingLocks;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.RegionLock;

@RegionLock("DataLock is rwLock protects data" /* is INCONSISTENT */)
public class Test {
  protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();
  protected int data;
  
  public void set(final C c, final int v) {
    // Test that we don't accidently look up the lock declared in this class
    c.wLock.lock(); // [a]
    try {
      data = v; // BAD
    } finally {
      c.wLock.unlock(); // [a]
    }
  }
  
  public int get(final C c) {
    // Test that we don't accidently look up the lock declared in this class
    c.rwLock.readLock().lock(); // [b]
    try {
      return data; // BAD
    } finally {
      c.rwLock.readLock().unlock(); // [b]
    }
  }
}
