package test.AnonymousClass.SuperIsNotInner;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;
import com.surelogic.SingleThreaded;

@RegionLocks({
  @RegionLock("COUNT is lockCount protects count"),
  @RegionLock("F is lockF protects f")
})
public class Super {
  public final Lock lockF = new ReentrantLock();
  public int f;
  
  public static final Lock lockCount = new ReentrantLock();
  public static int count = 0;
  
  @SingleThreaded
  @Borrowed("this")
  @RequiresLock("COUNT")
  public Super() {
    Super.count += 1;
    this.f = 10;
  }
}
