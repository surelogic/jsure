package intrinsic.classInit;

import com.surelogic.AggregateInRegion;
import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.Unique;

@RegionLock("Lock is class protects R")
@Region("static R")
public class Test {
  @InRegion("R")
  private static int f;

  @Unique
  @AggregateInRegion("R")
  private static final D d = new D();



  @RequiresLock("Lock")
  public static void m() {}

  static {
    // Class initializer message, normal region access
    f = 10;
    // Class initializer message, method precondition
    m();
    // Class initializer message, indirect region access
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
