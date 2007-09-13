package dont_sync_on_JUC_locks;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.RegionLock;

/**
 * Test that we flag cases where java.util.concurrent.locks.Lock objects are 
 * used with synchronized statements.  
 */
@RegionLock("VLock is jucLock protects value")
public class Test {
  private final ReadWriteLock jucLock = new ReentrantReadWriteLock();
  private final Object objLock = new Object();
  
  private int value;
  
  public void bad() {
    // Warning here: don't synchronize on JUC locks!
    synchronized (jucLock) {
      doStuff();
    }
  }

  public void good() {
    // warning: not identifiable as lock; okay to sync on object
    synchronized (objLock) {
      doStuff();
    }
  }
  
  private void doStuff() {
    // do stuff
  }
  
  public void bad2() {
    // Again, warning here: don't synchronize on JUC Locks!
    synchronized (jucLock) {
      // BAD: This field access should not be considered protected!
      value = 10;
    }
  }
}
