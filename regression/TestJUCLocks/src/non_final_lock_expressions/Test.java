package non_final_lock_expressions;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Test {
  /**
   * Shouldn't get any results for this method becaue the lock expression
   * isn't final.
   */
  public void good_nonFinalExpressions() {
    Lock L = new ReentrantLock();
    // Warning: L is non-final
    L.lock();
    // Warning: L is non-final
    L.unlock();
  }
}
