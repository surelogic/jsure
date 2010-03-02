package juc.good;

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
  public void m() {}

  public void doStuff() {
    jucLock.lock();
    try {
      // Good message, normal region access
      f = 10;
      // Good message, method precondition
      m();
      // Good message, indirect region access
      d.m();
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
