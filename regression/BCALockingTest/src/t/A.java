package t;

import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.Regions;
import com.surelogic.UniqueInRegion;

@Regions({
  @Region("protected A_R1"),
  @Region("protected A_R2")
})
@RegionLock("ALock is this protects A_R2")
public class A {
  @UniqueInRegion("B_R1 into A_R1, B_R2 into A_R2, Instance into Instance")
  protected final B b = new B();
  
  @Borrowed("this")
  @RegionEffects("none")
  public A() {
    super();
  }

  @Borrowed("this")
  protected void doStuff1(final int v1, final int v2) {
    this.b.c.d.e.f1 = v1;
    // Needs ALock
    this.b.c.d.e.f2 = v2;
  }

  @Borrowed("this")
  protected void doStuff2(final int v1, final int v2) {
    final B local_b = this.b;
    final C local_c = local_b.c;
    final D local_d = local_c.d;
    final E local_e = local_d.e;
    local_e.f1 = v1;
    // Needs ALock
    local_e.f2 = v2;
  }

  @Borrowed("this")
  protected void doStuff3(final int v1, final int v2) {
    final B local_b;
    if (v1 > v2) {
      local_b = B.getB();
    } else {
      local_b = this.b;
    }
    local_b.c.d.e.f1 = v1;
    
    /* Here we need lock this.ALock because local_b might cause aggregation
     * into the receiver.
     * 
     * We also need local_b.BLock because local_b might be a random B object
     * that is not aggregated into this A object, in which case the aggregation
     * causes the use of f2 to be reflected as a use of the B_R2 region of the
     * object referenced by local_b.
     */
    local_b.c.d.e.f2 = v2;
  }
}
