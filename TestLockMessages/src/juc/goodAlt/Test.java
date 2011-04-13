package juc.goodAlt;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.Unique;

@RegionLock("Lock is jucLock protects Instance")
public class Test {
  public final Lock jucLock = new ReentrantLock();

  private int f;

  @Unique
  private final D d = new D();



  private class Inner {
    // blah
  }



  @RequiresLock("Lock")
  public void m() {}

  public void doStuff() {
    jucLock.lock();
    try {
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
    } finally {
      jucLock.unlock();
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
