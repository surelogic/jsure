package tryLock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;

@Region("private R")
@RegionLock("L is lock protects R")
public class C {
  private final Lock lock = new ReentrantLock();
  
  @InRegion("R")
  private int f;
  
  
  
  public void doStuff() {
    if (lock.tryLock()) {
      try {
        f = 1; // good
      } finally {
        lock.unlock();
      }
      f = 10; // bad
    } else {
      f = 10; // bad
    }
  }
}
