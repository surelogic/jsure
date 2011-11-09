package misc.heldAs_unresolvable_tests;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;

@RegionLock("L is lock protects f")
public class C {
  public final Lock lock = new ReentrantLock();
  public int f;
  
  public class S {
  }
  
  public void stuff1(final C other) {
    final S s = other. new S() {
      {
        C.this.f = 10; // Lock is not held
      }
    };
  }

  public void stuff2(final C other) {
    other.lock.lock();
    try {
      final S s = other. new S() {
        {
          C.this.f = 10; // Lock "held as" other
        }
      };
    } finally {
      other.lock.unlock();
    }
  }

  public void stuff3(final C other) {
    final S s = other. new S() {
      {
        C.this.lock.lock();
        try {
          C.this.f = 10; // Normal lock held message
        } finally {
          C.this.lock.unlock();
        }
      }
    };
  }
}
