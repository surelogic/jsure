package intrinsic.classInitAlt;

import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;

@RegionLock("Lock is class protects R")
@Region("static R")
public class Test {
  @InRegion("R")
  private static int f;

  @UniqueInRegion("R")
  private static final D d = new D();



  private class Inner {
    // blah
  }



  @RequiresLock("Lock")
  public static void m() {}

  static {
    final Inner i = new Test(). new Inner() {
      {
        // Class initializer message, normal region access -- Alternative message not possible for class initializer because it deals with static locks
        f = 10;
        // Class initializer message, method precondition -- Alternative message not possible for class initializer because it deals with static locks
        m();
        // Class initializer message, indirect region access -- Alternative message not possible for class initializer because it deals with static locks
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
