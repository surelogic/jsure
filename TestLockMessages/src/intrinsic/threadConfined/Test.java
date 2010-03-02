package intrinsic.threadConfined;

import com.surelogic.Aggregate;
import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.Unique;

@RegionLock("Lock is this protects Instance")
public class Test {
  private int f;

  @Unique
  @Aggregate
  private final D d = new D();



  @RequiresLock("Lock")
  @Borrowed("this")
  public void m() {}

  @Unique("return")
  public Test() {
    // Thread-confined message, normal region access
    f = 10;
    // Thread-confined message, method precondition
    m();
    // Thread-confined message, indirect region access
    d.m();
  }
}

class D {
  @Unique("return")
  public D() {}

  @RegionEffects("writes Instance")
  @Borrowed("this")
  public void m() {}
}
