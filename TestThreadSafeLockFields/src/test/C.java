package test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;
import com.surelogic.ThreadSafe;
import com.surelogic.Unique;

/*
 * Test case for Bug 1708.  We need to make sure that we can have 
 * final Object lock fields.  @ThreadSafe assurance was rejecting these
 * because class Object is not a thread safe type, only a thread safe 
 * implementation.  But when a field is final and has an initializer, we 
 * can check the safety of the *implementation* of the type of the 
 * initializer.  This is okay because the field is final and we know it can
 * never be reinitialized.
 * 
 * Also check that JUC locks (Lock and ReadWRiteLock) do not trigger
 * problems with @ThreadSafe assurance.
 */

@ThreadSafe
@Regions({
  @Region("public R1"),
  @Region("public R2"),
  @Region("public R3"),
  @Region("public R4"),
  @Region("public R5"),
})
@RegionLocks({
  @RegionLock("L1 is this protects R1"),
  @RegionLock("L2 is lock1 protects R2"),
  @RegionLock("L3 is lock2 protects R3"),
  @RegionLock("L4 is lock3 protects R4"),
  @RegionLock("L5 is lock4 protects R5")
})
@SuppressWarnings("unused")
public class C {
  // GOOD
  public final Object lock1 = new Object();
  // BAD
  public final Object lock2 = new Bad();
  // GOOD
  public final Lock lock3 = new ReentrantLock();
  // GOOD
  public final ReadWriteLock lock4 = new ReentrantReadWriteLock();

  
  
  // BAD: We don't look at deferred initialization
  public final Object badEmpty;
  // BAD: We don't look at deferred initialization
  public final Bad badEmpty2;

  
  
  @InRegion("R1")
  private int x;
  
  @InRegion("R2")
  private int y;
  
  @InRegion("R3")
  private int z;
  
  @InRegion("R4")
  private int a;
  
  @InRegion("R5")
  private int b;
  
  
  
  @Unique("return")
  public C() {
    x = 1;
    y = 1;
    z = 1;
    a = 0;
    b = 0;
    
    badEmpty = new Bad();
    badEmpty2 = new Bad();
  }
}

class Bad {
  public Bad() {}
}
