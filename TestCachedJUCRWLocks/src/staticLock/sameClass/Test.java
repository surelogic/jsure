package staticLock.sameClass;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.RegionLock;

@RegionLock("DataLock is rwLock protects data" /* is CONSISTENT */)
public class Test {
  private final static ReadWriteLock rwLock = new ReentrantReadWriteLock();
  private final static Lock wLock = rwLock.writeLock();
  
  private static int data;
  
  public static void set(final int v) {
    wLock.lock(); // [a]
    try {
      data = v; // GOOD
    } finally {
      wLock.unlock(); // [a]
    }
  }
  
  public static int get() {
    rwLock.readLock().lock(); // [b]
    try {
      return data; // GOOD
    } finally {
      rwLock.readLock().unlock(); // [b]
    }
  }
}
