package instanceLock.sameClass;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.RegionLock;

@RegionLock("DataLock is rwLock protects data")
public class Test {
  private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
  private final Lock wLock = rwLock.writeLock();
  
  private int data;
  
  public void set(final int v) {
    wLock.lock(); // [a]
    try {
      data = v; // GOOD
    } finally {
      wLock.unlock(); // [a]
    }
  }
  
  public int get() {
    rwLock.readLock().lock(); // [a]
    try {
      return data; // GOOD
    } finally {
      rwLock.readLock().unlock(); // [a]
    }
  }
}
