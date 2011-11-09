package singleThreaded;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;


@RegionLocks({
  @RegionLock("L1 is l1 protects f1"),
  @RegionLock("L2 is l2 protects f2"),
  @RegionLock("L3 is l3 protects f3")
})
public class SingleThreadedTest {
  private final ReadWriteLock l1 = new ReentrantReadWriteLock();
  private final ReadWriteLock l2 = new ReentrantReadWriteLock();
  private final ReadWriteLock l3 = new ReentrantReadWriteLock();
  
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
  

  
  @Borrowed("this")
  public SingleThreadedTest() {
    // Holds L1.w, L2.w, L3.w
    // Protected: Needs L1.w
    f1 = 1;
    // Holds L1.w, L2.w, L3.w
    // Protected: Needs L2.w
    f2 = 2;
    
    // Holds L1.w, L2.w, L3.w
    // Protected: needs L1.r
    int x = f1;
    // Holds L1.w, L2.w, L3.w
    // Protected: needs L2.r
    int y = f2;    
  }

  public SingleThreadedTest(int bad) {
    // UNPROTECTED!
    f1 = 1;
    // UNPROTECTED!
    f2 = 2;
  }
}
