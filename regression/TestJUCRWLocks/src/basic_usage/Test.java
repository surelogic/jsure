package basic_usage;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.RegionLock;
import com.surelogic.InRegion;
import com.surelogic.Region;

@Region("protected Value")
@RegionLock("L is rwLock protects Value")
public class Test {
  protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();
  @InRegion("Value")
  private int value1;
  @InRegion("Value")
  private int value2;
  
  public void set1(int v) {
    rwLock.writeLock().lock();
    try {
      // GOOD: Needs write, has write
      value1 = v;
    } finally {
      rwLock.writeLock().unlock();
    }
  }
  
  public int get1() {
    rwLock.readLock().lock();
    try {
      // HOLDS L.readLock()
      // GOOD: Needs read, has read
      return value1;
    } finally {
      // HOLDS L.readLock()
      rwLock.readLock().unlock();
    }
  }
  
  public void set2(int v) {
    rwLock.readLock().lock();
    try {
      // HOLDS L.readLock()
      // BAD: Needs write, has read
      value2 = v;
    } finally {
      // HOLDS L.readLock()
      rwLock.readLock().unlock();
    }
  }
  
  public int get2() {
    rwLock.writeLock().lock();
    try {
      // HOLDS L.writeLock()
      // GOOD: Needs read, has write
      return value2;
    } finally {
      // HOLDS L.writeLock()
      rwLock.writeLock().unlock();
    }
  }
  
  
  public void mixedUsed() {
    rwLock.writeLock().lock();
    try {
      // HOLDS L.writeLock()
      // GOOD: Holds write lock; write
      value1 = 
        // HOLDS L.writeLock()
        // GOOD: Holds write lock; read
        value2;
    } finally {
      // HOLDS L.writeLock()
      rwLock.writeLock().unlock();
    }
    
    rwLock.readLock().lock();
    try {
      // HOLDS L.readLock()
      // BAD: Holds read lock; write
      value1 = 
        // HOLDS L.readLock()
        // GOOD: Holds read lock; read
        value2;
    } finally {
      // HOLDS L.readLock()
      rwLock.readLock().unlock();
    }
  }
}
