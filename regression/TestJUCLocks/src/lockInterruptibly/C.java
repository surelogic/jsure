package lockInterruptibly;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;


/**
 * Test that analysis works with lockInterruptibly().
 */
@Region("private R")
@RegionLock("L is lock protects R" /* is CONSISTENT */)
public class C {
  private final Lock lock = new ReentrantLock();
  
  @InRegion("R")
  private int f;
  
  public void test() throws InterruptedException {
    lock.lockInterruptibly();
    try {
      // PROTECTED
      f = 1;
    } finally {
      lock.unlock();
    }
  }

  
  public void test2(boolean flag) throws InterruptedException {
    if (flag) {
      lock.lock();
    } else {
      lock.lockInterruptibly();
    }      
    try {
      // PROTECTED
      f = 1;
    } finally {
      lock.unlock();
    }
  }
}
