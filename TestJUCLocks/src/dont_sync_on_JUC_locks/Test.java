package dont_sync_on_JUC_locks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Test that we flag cases where java.util.concurrent.locks.Lock objects are 
 * used with synchronized statements.  
 */
@com.surelogic.Lock("VLock is jucLock protects value")
public class Test {
  private final Lock jucLock = new ReentrantLock();
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
