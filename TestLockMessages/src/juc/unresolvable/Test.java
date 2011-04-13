package juc.unresolvable;

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
    private class Inner2 {
      // blah
    }

    private class Inner3 {
      private class Inner4 {
        public void doStuff(Inner in) {
          final Inner.Inner2 i2 = in.new Inner2() {
            {
              // Unresolvable message, normal region access
              f = 10;
              // Unresolvable message, method precondition
              m();
              // Unresolvable message, indirect region access
              d.m();
            }
          };
        }
      }
    }
  }

  @RequiresLock("Lock")
  public void m() {}
}

class D {
  @Unique("return")
  public D() {}

  @RegionEffects("writes Instance")
  @Borrowed("this")
  public void m() {}
}
