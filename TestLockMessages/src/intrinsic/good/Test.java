package intrinsic.good;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.Unique;

@RegionLock("Lock is this protects Instance")
public class Test {
  private int f;

  @Unique
  private final D d = new D();



  @RequiresLock("Lock")
  public void m() {}

  public void doStuff() {
    synchronized (this) {
      // Good message, normal region access
      f = 10;
      // Good message, method precondition
      m();
      // Good message, indirect region access
      d.m();
    }
  }
}

class D {
  @Unique("return")
  public D() {}

  @RegionEffects("writes Instance")
  @Borrowed("this")
  public void m() {}
}
