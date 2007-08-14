package not_just_any_RWLock_class;

/**
 * Test that only methods from java.util.concurrent.locks.Lock are used by
 * the analysis.  This class calls lock() and unlock() from class
 * not_just_any_Lock_class.Lock, and they are ignored by the analysis.
 */
public class Main {
  private final ReaderWriterLock lock = new ReaderWriterLock();
  
  public void testRead() {
    // Warning: not from the Lock class
    lock.readLock().lock();
    try {
      doStuff();
    } finally {
      // Warning: not from the Lock class
      lock.readLock().unlock();
    }
  }
  
  public void testWrite() {
    // Warning: not from the Lock class
    lock.writeLock().lock();
    try {
      doStuff();
    } finally {
      // Warning: not from the Lock class
      lock.writeLock().unlock();
    }
  }
  
  private void doStuff() {
    // do stuff
  }
}
