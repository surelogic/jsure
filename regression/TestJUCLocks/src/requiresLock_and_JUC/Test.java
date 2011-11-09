package requiresLock_and_JUC;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLocks;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.RequiresLock;

/**
 * Test lock preconditions with JUC locks, and mixed JUC and intrinsic locks.
 */
@Regions({
  @Region("private Region1"),
  @Region("private Region2")
})
@RegionLocks({
  @com.surelogic.RegionLock("JUC_LOCK is lockField protects Region1"),
  @com.surelogic.RegionLock("JAVA_LOCK is objField protects Region2")
})
public class Test {
  private final Lock lockField = new ReentrantLock();
  private final Object objField = new Object();
  
  @InRegion("Region1")
  private int x;

  @InRegion("Region2")
  private int y;
  
 
  @RequiresLock("JUC_LOCK")
  private void requiresJUCLock(int v) {
    // Holds JUC_LOCK
    x = v;
  }
  
  public void goodCaller_JUCLock(int v) {
    lockField.lock();
    try {
      // Holds JUC_LOCK
      // GOOD: Holds the required lock
      requiresJUCLock(v);
    } finally {
      // Holds JUC_LOCK
      lockField.unlock();
    }
  }
  
  public void badCaller_JUCLock(int v) {
    // BAD: does not hold the required lock
    requiresJUCLock(v);
  }

  
  
  
  @RequiresLock("JAVA_LOCK")
  private void requiresJavaLock(int v) {
    y = v;
  }
  
  public void goodCaller_JavaLock(int v) {
    synchronized (objField) {
      // GOOD: Holds the lock
      requiresJavaLock(v);
    }
  }
  
  public void badCaller_JavaLock(int v) {
    // BAD: Doesn't hold the lock
    requiresJavaLock(v);
  }

  
  
  @RequiresLock("JUC_LOCK, JAVA_LOCK")
  private void requiresBoth(int a, int b) {
    // Holds JUC_LOCK
    x = a;
    // Holds JUC_LOCK
    y = b;
  }
  
  public void goodCaller_requiresBoth(int a, int b) {
    synchronized (objField) {
      lockField.lock();
      try {
        // Holds JUC_LOCK
        // GOOD: Caller holds both JUC_LOCK and JAVA_LOCK
        requiresBoth(a, b);
      } finally {
        // Holds JUC_LOCK
        lockField.unlock();
      }
    }
  }
  
  public void badCaller_requiresBoth(int a, int b) {
    // BAD: Caller doesn't hold either of the required locks
    requiresBoth(a, b);
  }
}
