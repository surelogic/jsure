package aggregation.nonFinalField;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
    @Region("public F"),
    @Region("public G"),
    @Region("public H")
})
//@RegionLocks({
//  @RegionLock("FL is this protects F"),
//  @RegionLock("GL is this protects G"),
//  @RegionLock("HL is this protects H")
//})
public class Outer {
  @InRegion("F")
  private C f = new C();

  @InRegion("G")
  private C g = new C();

  @InRegion("H")
  private C h = new C();

//  public Outer() {}

  public void m() {
    f.x = 1;
    g.x = 2;
    h.x = 3;
  }
}

class C {
  @Unique("return")
  public C() {}

  public int x;
  // blah
}