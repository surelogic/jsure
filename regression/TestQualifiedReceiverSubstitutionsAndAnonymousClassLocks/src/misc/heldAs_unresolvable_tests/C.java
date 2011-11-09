package misc.heldAs_unresolvable_tests;

import com.surelogic.RegionLock;

@RegionLock("L is this protects f")
public class C {
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
    synchronized (other) {
      final S s = other. new S() {
        {
          C.this.f = 10; // Lock "held as" other
        }
      };
    }
  }

  public void stuff3(final C other) {
    final S s = other. new S() {
      {
        synchronized (C.this) {
          C.this.f = 10; // Normal lock held message
        }
      }
    };
  }
}
