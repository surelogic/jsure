package intrinsic.bad;

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



  private class Inner {
    // blah
  }



  @RequiresLock("Lock")
  public void m() {}

  public void doStuff() {
    // Bad message, no alternative available, normal region access
    f = 10;
    // Bad message, no alternative available, method precondition
    m();
    // Bad message, no alternative available, indirect region access
    d.m();

    final Inner i = new Inner() {
      {
        // Bad message, alternative available but not held, normal region access
        f = 10;
        // Bad message, alternative available but not held, method precondition
        m();
        // Bad message, alternative available but not held, indirect region access
        d.m();
      }
    };
  }
}

class D {
  @Unique("return")
  public D() {}

  @RegionEffects("writes Instance")
  @Borrowed("this")
  public void m() {}
}
