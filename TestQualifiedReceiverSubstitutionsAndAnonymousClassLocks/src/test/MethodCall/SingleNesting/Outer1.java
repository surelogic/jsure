package test.MethodCall.SingleNesting;

import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;

@RegionLocks({
  @RegionLock("F1 is lockF1 protects f1"),
  @RegionLock("F2 is lockF2 protects f2")
})
public class Outer1 {
  public final Object lockF1 = new Object();
  public final Object lockF2 = new Object();
  public int f1;
  public int f2;

  @RegionLock("F3 is lockF3 protects f3")
  public class Nested {
    public final Object lockF3 = new Object();
    public int f3;
    
    @RequiresLock("this:F3, Outer1.this:F1, Outer1.this:F2")
    public int doStuff() {
      return
        this.f3 + // Assures
        Outer1.this.f1 + // Assures
        Outer1.this.f2; // Assures
    }
    
    @RequiresLock("this:F3, Outer1.this:F1, Outer1.this:F2")
    public int testReceiverIsThis() {
      // Use of qualified receiver is still reported as qualified receiver
      return this.doStuff(); // All three locks assure
    }
  }
  
  public class E1 extends Nested {
    @RequiresLock("this:F3, Outer1.this:F1, Outer1.this:F2")
    public int testReceiverIsThis2() {
      // Use of qualified receiver is still reported as qualified receiver
      return this.doStuff(); // All three locks assure
    }
    
    @RequiresLock("this:F3, Outer1.this:F1, Outer1.this:F2")
    public int testReceiverIsSuper() {
      // Use of qualified receiver is still reported as qualified receiver
      return super.doStuff(); // All three locks assure
    }
  }
  
  
  @RequiresLock("n1:F3, n2:F3")
  public static int test(final Nested n1, final Nested n2) {
    return
      // Cannot map qualified receiver
      n1.doStuff() + // F3 assures, F1 and F2 cannot be resolved
      // Cannot map qualified receiver
      n2.doStuff(); // F3 assures, F1 and F2 cannot be resolved
  }
}

