package not_just_any_RWLock_class;

import java.util.concurrent.locks.Lock;

/**
 * Bogus lock class used by class main().  
 */
public class ReaderWriterLock {
  public Lock readLock() { return null; }
  public Lock writeLock() { return null; }
}

