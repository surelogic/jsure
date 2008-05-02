package test.MethodCall.NoNesting;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;

@RegionLocks({
  @RegionLock("F1 is lockF1 protects f1"),
  @RegionLock("F2 is lockF2 protects f2")
})
public class NormalCall {
  public final Lock lockF1 = new ReentrantLock();
  public final Lock lockF2 = new ReentrantLock();
  public int f1;
  public int f2;

  @RequiresLock("NormalCall.this:F1, this:F2")
  public int doStuff1() {
    return
      f1 + // Assures
      NormalCall.this.f2; // Assures
  }

  public int doStuff2() {
    lockF1.lock();
    try {
      lockF2.lock();
      try {
        return
        f1 + // Assures
        NormalCall.this.f2; // Assures
      } finally {
        lockF2.unlock();
      }
    } finally {
      lockF1.unlock();
    }
  }

  @RequiresLock("a:F1, a:F2, b:F1, b:F2")
  public static int test1(final NormalCall a, final NormalCall b) {
    return
      a.doStuff1() + // both locks assure
      b.doStuff1(); // both locks assure
  }

  public static int test2(final NormalCall a, final NormalCall b) {
    a.lockF1.lock();
    try {
      a.lockF2.lock();
      try {
        b.lockF1.lock();
        try {
          b.lockF2.lock();
          try {
            return
              a.doStuff1() + // both locks assure
              b.doStuff1(); // both locks assure
          } finally {
            b.lockF2.unlock();
          }
        } finally {
          b.lockF1.unlock();
        }
      } finally {
        a.lockF2.unlock();
      }
    } finally {
      a.lockF1.unlock();
    }
  }
}
