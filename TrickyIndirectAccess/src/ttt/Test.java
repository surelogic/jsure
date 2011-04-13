package ttt;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.UniqueInRegion;

@RegionLocks({
  @RegionLock("L1 is l1 protects R1"),
  @RegionLock("L2 is l2 protects R2")
})
@Regions({
  @Region("public static S1"),
  @Region("public static S2 extends S1"),
  @Region("public R1 extends S2"),
  @Region("public R2 extends S2")
})
public class Test {
  public final Object l1 = new Object();
  public final Object l2 = new Object();

  @UniqueInRegion("Instance into S2, f1 into R1, f2 into R2")
  private final Inner f = new Inner();
  
  @Borrowed("this")
  public Test() {
    super();
  }

  @RegionEffects("writes ttt.Test:S2")
  public void doStuff() {
    // Needs this:L1, this:L2, even though the original effect here is the
    // writes(Test:S2)---an effect on a static region
    this.f.setBoth(5, 10);
  }
}
