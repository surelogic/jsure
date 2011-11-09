package unidentifiable_locks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Test identification of final lock expressions that don't refer to any
 * declared locks.
 */
@com.surelogic.RegionLock("LL is identifiable protects Instance")
public class Test {
  public final Lock identifiable = new ReentrantLock();
  public final Lock unidentifiable = new ReentrantLock();
  
  public void noWarning() {
    identifiable.lock(); // matches [*]
    try {
      // do stuff
    } finally {
      // Holds LL
      identifiable.unlock(); // matches [*]
    }
  }

  public void warning() {
    // Unidentifiable lock expression
    unidentifiable.lock(); // matches [**]
    try {
      // do stuff
    } finally {
      // Unidentifiable lock expression
      unidentifiable.unlock(); // matches [**]
    }
  }
}
