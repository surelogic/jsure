package test.ConstructorCall.Unqualified.SingleNesting;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;
import com.surelogic.SingleThreaded;

@RegionLocks({
  @RegionLock("F1 is lockF1 protects f1"),
  @RegionLock("F2 is lockF2 protects f2")
})
public class Outer1 {
  public final Object lockF1 = new Object();
  public final Object lockF2 = new Object();
  public int f1;
  public int f2;

  @SingleThreaded
  @Borrowed("this")
  public Outer1() {
    f1 = 0;
    f2 = 1;
  }

  @RegionLock("F3 is lockF3 protects f3")
  public class Nested {
    public final Object lockF3 = new Object();
    public int f3;
    
    @RequiresLock("Outer1.this:F1, Outer1.this:F2")
    @SingleThreaded
    @Borrowed("this")
    public Nested(final int y) {
      this.f3 = y;
      Outer1.this.f1 = y + 1;
      Outer1.this.f2 = y + 2;
    }
    
    @RequiresLock("Outer1.this:F1, Outer1.this:F2")
    @SingleThreaded
    @Borrowed("this")
    public Nested() {
      // Effects on qualified receiver are still reported as effects on qualified receiver
      this(10);
    }
  }
  
  public class E1 extends Nested {
    @RequiresLock("Outer1.this:F1, Outer1.this:F2")
    @SingleThreaded
    @Borrowed("this")
    public E1(final int y) {
      // Effects on qualified receiver are still reported as effects on qualified receiver
      super(y);
    }

    @RequiresLock("Outer1.this:F1, Outer1.this:F2")
    @SingleThreaded
    @Borrowed("this")
    public E1() {
      // Effects on qualified receiver are still reported as effects on qualified receiver
      this(100);
    }
  }
}

