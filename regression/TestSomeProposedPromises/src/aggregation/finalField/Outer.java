package aggregation.finalField;

import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
    @Region("public F"),
    @Region("public G"),
    @Region("public H")
})
@RegionLocks({
  @RegionLock("FL is this protects F"),
  @RegionLock("GL is this protects G"),
  @RegionLock("HL is this protects H")
})
public class Outer {
  private final C f = new C();

  public void m() {
    f.x = 1;
  }
}

class C {
  @Unique("return")
  public C() {}

  public int x;
  // blah
}