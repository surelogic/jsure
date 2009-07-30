package t;

import com.surelogic.Aggregate;
import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
  @Region("protected B_R1"),
  @Region("protected B_R2")
})
@RegionLock("BLock is this protects B_R2")
public class B {
  @Unique
  @Aggregate("C_R1 into B_R1, C_R2 into B_R2, Instance into Instance")
  protected final C c = new C();
  
  @Borrowed("this")
  @RegionEffects("none")
  public B() {
    super();
  }
  
  public static B getB() {
    return new B();
  }

  @Borrowed("this")
  protected void doStuff1(final int v1, final int v2) {
    this.c.d.e.f1 = v1;
    // Needs BLock
    this.c.d.e.f2 = v2;
  }
  
  @Borrowed("this")
  protected void doStuff2(final int v1, final int v2) {
    final C local_c = this.c;
    final D local_d = local_c.d;
    final E local_e = local_d.e;
    local_e.f1 = v1;
    // Needs BLock
    local_e.f2 = v2;
  }

  @Borrowed("this")
  protected void doStuff3(final int v1, final int v2) {
    final C local_c;
    if (v1 > v2) {
      local_c = C.getC();
    } else {
      local_c = this.c;
    }
    local_c.d.e.f1 = v1;
    
    /* Here we need lock this.BLock because local_c might cause aggregation
     * into the receiver.
     * 
     * We also need local_c.CLock because local_c might be a random C object
     * that is not aggregated into this C object, in which case the aggregation
     * causes the use of f2 to be reflected as a use of the C_R2 region of the
     * object referenced by local_c.
     */
    local_c.d.e.f2 = v2;
  }
}
