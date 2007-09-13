package requiresLock_and_JUC;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RequiresLock;

/**
 * Test lock preconditions on JUC locks for method parameters (other than
 * the receiver).
 */
@com.surelogic.RegionLock("Lock is lockField protects Instance")
public class Other {
  public final Lock lockField = new ReentrantLock();
  
  private int value;
  
  /**
   * Requires a JUC lock on the receiver.
   */
  @RequiresLock("Lock")
  public void set(int v) {
    // Holds LOCK
    value = v;
  }
  
  /**
   * Requires a JUC lock on the receiver.
   */
  @RequiresLock("Lock")
  public int get() {
    // Holds LOCK
    return value;
  }
  
  
  
  /**
   * Requires a JUC lock on the parameter "o".
   */
  @RequiresLock("o:Lock")
  public static void add(int v, final Other o) {
    // Holds o.LOCK
    // GOOD: Caller holds Lock
    o.set(v +
        // Holds o.LOCK
        // GOOD: Caller holds Lock
        o.get());
  }
  
  public static void doIt() {
    final Other other = new Other();
    
    // bad
    other.set(10);
    
    // good
    other.lockField.lock();  
    try {
      // Holds other.LOCK
      // GOOD: Caller holds lock on receiver
      other.set(100);
    } finally {
      // Holds other.LOCK
      other.lockField.unlock();
    }
    
    // bad
    // BAD: Caller doesn't hold lock
    add(5, other);
    
    // good
    other.lockField.lock();  
    try {
      // Holds other.LOCK
      // GOOD: Caller holds lock on "other"
      add(5, other);
    } finally {
      // Holds other.LOCK
      other.lockField.unlock();
    }
  }
}
