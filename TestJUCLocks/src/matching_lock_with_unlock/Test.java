package matching_lock_with_unlock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Test {
  private final Lock lock1 = new ReentrantLock();
  private final Lock lock2 = new ReentrantLock();
  
  private void doStuff() {
    // do work here.
  }
  
  public void good_tryFinally_convention() {
    lock1.lock();  // matches [a]
    try {
      doStuff();
    } finally {
      lock1.unlock(); // matches [a]
    }
  }
  
  public void bad_noFinally() {
    lock1.lock();  // Different # of matching unlocks() because of possible exception by doStuff()
    doStuff();
    lock1.unlock();  // missing lock() because of possible exception by doStuff()
  }
  
  public void good_specialCase() {
    // GOOD: No finally is required because the critical section is empty
    lock1.lock();  // matches [b]
    // empty critical section
    lock1.unlock();  // matches [b]
  }
  
  public void bad_missingLock() {
    lock1.unlock();  // missing lock()
  }
  
  public void bad_missingUnlock() {
    lock1.lock();  // missing unlock()
  }
  
  public void bad_mismatchedLocks() {
    // Same as missingLock + missingUnlock
    lock1.lock(); // missing unlock()
    try {
      doStuff();
    } finally {
      lock2.unlock(); // missing lock()
    }
  }
  
  // lock1 is acquired 2 times, released 2 times, lock2 is acquired and released once
  public void good_nesting() {
    lock1.lock();  // matches [f]
    try {
      lock1.lock();  // matches [e]
      try {
        lock2.lock();  // matches [d]
        try {
          lock2.lock();  // matches [c]
          lock2.unlock();  // matches [c]
        } finally {
          lock2.unlock();  // matches [d]
        }
      } finally {
        lock1.unlock();  // matches [e]
      }
    } finally {
      lock1.unlock();  // matches [f]
    }
  }

  // Lock1 is acquired 4 times, released 2 times
  public void bad_nesting() {
    lock1.lock();  // Different # of matching unlocks() because of bad nesting
    try {
      lock1.lock(); // missing unlock()
      try {
        lock1.lock(); // matches [i]
        try {
          lock1.lock();  // matches [h]
          try {
            lock2.lock();  // matches [g]
            lock2.unlock();  // matches [g]
          } finally {
            lock1.unlock(); // matches [h]
          }
        } finally {
          lock1.unlock(); // matches [i]
        }
      } finally {
      }
    } finally {
    }
  }
}
