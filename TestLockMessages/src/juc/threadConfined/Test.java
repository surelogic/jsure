package juc.threadConfined;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Aggregate;
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
