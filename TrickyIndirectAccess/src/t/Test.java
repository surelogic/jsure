package t;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.Aggregate;
import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.SingleThreaded;
import com.surelogic.Unique;

@RegionLocks({
  @RegionLock("RW2 is rwLock2 protects Region2"),
  @RegionLock("RW3 is rwLock3 protects Region3")
})
@Regions({
  @Region("private Region2"),
  @Region("private Region3")
})
public class Test {
  private final ReadWriteLock rwLock2 = new ReentrantReadWriteLock();
  private final ReadWriteLock rwLock3 = new ReentrantReadWriteLock();

  @Unique
  @Aggregate("Instance into Instance, f1 into Region2, f2 into Region3")
  private final Inner f = new Inner();
  
  @SingleThreaded
  @Borrowed("this")
  public Test() {
    super();
  }
  
  public void doStuff() {
    /* Needs lock RW2 and RW3 because 
     * - The field is Unique and its state is aggregated into the state of the
     *   Test object.
     * - The method affects the region Instance of the object referenced by this.f.
     * - The region Instance of the object referenced by this.f contains the
     *   regions f1, and f2
     * - The regions f1 and f2 are aggregated into the regions Region2 and
     *   Region3 of the referencing Test object.
     * - Region2 needs lock RW2
     * - Region3 needs lock RW3
     */
    this.f.setBoth(5, 10);
  }
}
