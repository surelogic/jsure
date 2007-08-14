package returnsLock_and_JUC;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.Lock;
import com.surelogic.Locks;
import com.surelogic.MapInto;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.ReturnsLock;

@Locks({
  @Lock("L is lockField protects Region1"),
  @Lock("M is lockField2 protects Region2")
})
@Regions({
  @Region("Region1"),
  @Region("Region2")
})
public class Test {
  private final ReadWriteLock lockField = new ReentrantReadWriteLock();
  private final ReadWriteLock lockField2 = new ReentrantReadWriteLock();
  private final Object object = new Object();
  
  @MapInto("Region1")
  private int value;
  
  
  /**
   * Test that returnsLock works with JUC locks
   */
  @ReturnsLock("L")
  public ReadWriteLock getLock() {
    // GOOD: Returns correct lock
    return lockField;
  }
  
  /**
   * Test that returnsLock works with JUC locks
   */
  @ReturnsLock("M")
  public ReadWriteLock getLock2() {
    // GOOD: Returns correct lock
    return lockField2;
  } 
  
  /**
   * Test that returnsLock works with JUC locks---Do we detect when the wrong
   * lock is returned?
   */
  @ReturnsLock("L")
  public ReadWriteLock wrongLock() {
    // BAD: Returns wrong lock
    return lockField2;
  }
  
  /**
   * Test that returnsLock works with JUC locks---Do we get confused when the 
   * returned lock is an intrinsic lock?
   */
  @ReturnsLock("M")
  public Object wrongLock2() {
    // BAD: returns wrong lock
    return object;
  }
  
  /**
   * Test lock()/unlock() processing with lock getter methods.
   */
  public void setValue(int v) {
    getLock().writeLock().lock();
    try {
      // Holds L
      // GOOD: Protected access
      value = v;
    } finally {
      // Holds L
      getLock().writeLock().unlock();
    }
  }

  /**
   * Test lock()/unlock() processing with lock getter methods: Do we detect
   * when the wrong lock is acquired?
   */
  public int getValue() {
    getLock2().readLock().lock();
    try {
      // Holds M
      // BAD: Wrong lock
      return value;
    } finally {
      // Holds M
      getLock2().readLock().unlock();
    }
  }
}
