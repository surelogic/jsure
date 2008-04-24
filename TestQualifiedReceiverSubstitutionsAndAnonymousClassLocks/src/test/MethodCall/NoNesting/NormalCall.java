package test.MethodCall.NoNesting;

import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;

@RegionLocks({
  @RegionLock("F1 is lockF1 protects f1"),
  @RegionLock("F2 is lockF2 protects f2")
})
public class NormalCall {
  public final Object lockF1 = new Object();
  public final Object lockF2 = new Object();
  public int f1;
  public int f2;

  @RequiresLock("NormalCall.this:F1, this:F2")
  public int doStuff() {
    return
      f1 + // Assures
      NormalCall.this.f2; // Assures
  }

  @RequiresLock("a:F1, a:F2, b:F1, b:F2")
  public static int test(final NormalCall a, final NormalCall b) {
    return
      a.doStuff() + // both locks assure
      b.doStuff(); // both locks assure
  }
}
