package test.AnonymousClass.SuperIsNotInner;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;

@RegionLocks({
  @RegionLock("COUNT is lockCount protects count"),
  @RegionLock("F is lockF protects f")
})
public class Super {
  public final Object lockF = new Object();
  public int f;
  
  public static final Object lockCount = new Object();
  public static int count = 0;
  
  @RequiresLock("COUNT")
  public static void accessCount() {}
  
  @RequiresLock("F")
  @Borrowed("this")
  public void accessF() {}
  
  @Borrowed("this")
  @RequiresLock("COUNT")
  public Super() {
    accessCount();
    accessF();
  }
}
