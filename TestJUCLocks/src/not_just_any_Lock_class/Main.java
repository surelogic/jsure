package not_just_any_Lock_class;

/**
 * Test that only methods from java.util.concurrent.locks.Lock are used by
 * the analysis.  This class calls lock() and unlock() from class
 * not_just_any_Lock_class.Lock, and they are ignored by the analysis.
 */
public class Main {
  private final Lock lock = new Lock();
  
  public void test() {
    // Warning: not from the Lock class
    lock.lock();
    try {
      doStuff();
    } finally {
      // Warning: not from the Lock class
      lock.unlock();
    }
  }
  
  private void doStuff() {
    // do stuff
  }
}
