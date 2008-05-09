package staticLock.duelingLocks;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.RegionLock;

@RegionLock("DataLock is rwLock protects data" /* is INCONSISTENT */)
public class Test {
  protected static final ReadWriteLock rwLock = new ReentrantReadWriteLock();
  protected static int data;
  
  public static void set(final int v) {
    // Test that we don't accidently lookup the lock declared in this class
    C.wLock.lock(); // [a]
    try {
      data = v; // BAD
    } finally {
      C.wLock.unlock(); // [a]
    }
  }
  
  public static int get() {
    // Test that we don't accidently lookup the lock declared in this class
    C.rwLock.readLock().lock(); // [b]
    try {
      return data; // BAD
    } finally {
      C.rwLock.readLock().unlock(); // [b]
    }
  }
}
