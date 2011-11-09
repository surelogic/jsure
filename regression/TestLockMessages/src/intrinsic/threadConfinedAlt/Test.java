package intrinsic.threadConfinedAlt;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.Starts;
import com.surelogic.Unique;

@RegionLock("Lock is this protects Instance")
public class Test {
  private int f;

  @Unique
  private final D d = new D();



  private class Inner {
    @RegionEffects("none")
    @Starts("nothing")
    public Inner() {}
    // blah
  }



  @RequiresLock("Lock")
  @Borrowed("this")
  @Starts("nothing")
  @RegionEffects("writes Instance")
  public void m() {}

  @Starts("nothing")
  @RegionEffects("none")
  public Test() {
    final Inner i = new Inner() {
      {
        // Thread-confined alternative message, normal region access
        f = 10;
        // Thread-confined alternative message, method precondition
        m();
        // Thread-confined alternative message, indirect region access
        d.m();
      }
    };
  }
}

class D {
  @Unique("return")
  @RegionEffects("none")
  public D() {}

  @RegionEffects("writes Instance")
  @Borrowed("this")
  @Starts("nothing")
  public void m() {}
}
