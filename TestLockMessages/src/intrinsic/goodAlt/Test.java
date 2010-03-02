package intrinsic.goodAlt;

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



  private class Inner {
    // blah
  }



  @RequiresLock("Lock")
  public void m() {}

  public void doStuff() {
    synchronized (this) {
      final Inner i = new Inner() {
        {
          // Good alternative message, normal region access
          f = 10;
          // Good alternative message, method precondition
          m();
          // Good alternative message, indirect region access
          d.m();
        }
      };
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
