package returnsLock_and_JUC;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLocks;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.ReturnsLock;

@RegionLocks({
  @com.surelogic.RegionLock("L is lockField protects Region1"),
  @com.surelogic.RegionLock("M is lockField2 protects Region2")
})
@Regions({
  @Region("Region1"),
  @Region("Region2")
})
public class Test {
  private final Lock lockField = new ReentrantLock();
  private final Lock lockField2 = new ReentrantLock();
  private final Object object = new Object();
  
  @InRegion("Region1")
  private int value;
  
  
  /**
   * Test that returnsLock works with JUC locks
   */
  @ReturnsLock("L" /* is CONSISTENT */)
  public Lock getLock() {
    // GOOD: Returns correct lock
    return lockField;
  }
  
  /**
   * Test that returnsLock works with JUC locks
   */
  @ReturnsLock("M" /* is CONSISTENT */)
  public Lock getLock2() {
    // GOOD: Returns correct lock
    return lockField2;
  } 
  
  /**
   * Test that returnsLock works with JUC locks---Do we detect when the wrong
   * lock is returned?
   */
  @ReturnsLock("L" /* is INCONSISTENT: Returns wrong lock */)
  public Lock wrongLock() {
    // BAD: Returns wrong lock
    return lockField2;
  }
  
  /**
   * Test that returnsLock works with JUC locks---Do we get confused when the 
   * returned lock is an intrinsic lock?
   */
  @ReturnsLock("M" /* is INCONSISTENT: Returns wrong lock */)
  public Object wrongLock2() {
    // BAD: returns wrong lock
    return object;
  }
  
  /**
   * Test lock()/unlock() processing with lock getter methods.
   */
  public void setValue(int v) {
    getLock().lock();  // [*]
    try {
      // Holds L
      // GOOD: Protected access
      value = v;
    } finally {
      // Holds L
      getLock().unlock(); // [*]
    }
  }

  /**
   * Test lock()/unlock() processing with lock getter methods: Do we detect
   * when the wrong lock is acquired?
   */
  public int getValue() {
    getLock2().lock(); // [**]
    try {
      // Holds M
      // BAD: Wrong lock
      return value;
    } finally {
      // Holds M
      getLock2().unlock(); // [**]
    }
  }
}
