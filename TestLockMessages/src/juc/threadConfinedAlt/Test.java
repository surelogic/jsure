package juc.threadConfinedAlt;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Aggregate;
import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.Starts;
import com.surelogic.Unique;

@RegionLock("Lock is jucLock protects Instance")
public class Test {
  public final Lock jucLock = new ReentrantLock();

  private int f;

  @Unique
  @Aggregate
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
