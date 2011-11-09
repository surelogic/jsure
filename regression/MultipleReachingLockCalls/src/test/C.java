package test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;

/**
 * Example class that has multiple lock calls that reach a single unlock call.
 */
@RegionLock("L is lock protects Instance")
public class C {
  public final Lock lock = new ReentrantLock();
  public int something;
  
  public void method(final boolean allowInterrupt) throws InterruptedException {
    if (allowInterrupt) {
      lock.lock();
    } else {
      lock.lockInterruptibly();
    }
    try {
      something = 1;
    } finally {
      lock.unlock();
    }
  }
}
