package singleThreaded;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Borrowed;
import com.surelogic.RegionLocks;
import com.surelogic.SingleThreaded;

@RegionLocks({
  @com.surelogic.RegionLock("L1 is l1 protects f1"),
  @com.surelogic.RegionLock("L2 is l2 protects f2"),
  @com.surelogic.RegionLock("L3 is l3 protects f3")
})
public class SingleThreadedTest {
  private final Lock l1 = new ReentrantLock();
  private final Lock l2 = new ReentrantLock();
  private final Lock l3 = new ReentrantLock();
  
  private int f1;
  private int f2;
  
  /* When analyzed on behalf of SingledThreaded():
   *   Holds L1, L2, L3
   *   Protected
   */
  /* When analyzed on behalf of SingledThreaded(int):
   *   UNPROTECTED
   */
  private int f3 = 100;
  

  
  @SingleThreaded
  @Borrowed("this")
  public SingleThreadedTest() {
    // Holds L1, L2, L3
    // Protected
    f1 = 1;
    // Holds L1, L2, L3
    // Protected
    f2 = 2;
  }

  public SingleThreadedTest(int bad) {
    // UNPROTECTED!
    f1 = 1;
    // UNPROTECTED!
    f2 = 2;
  }
}
