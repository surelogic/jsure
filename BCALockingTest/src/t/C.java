package t;

import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.Regions;
import com.surelogic.UniqueInRegion;

@Regions({
  @Region("protected C_R1"),
  @Region("protected C_R2")
})
@RegionLock("CLock is this protects C_R2")
public class C {
  @UniqueInRegion("D_R1 into C_R1, D_R2 into C_R2, Instance into Instance")
  protected final D d = new D();
  
  @Borrowed("this")
  @RegionEffects("none")
  public C() {
    super();
  }
  
  public static C getC() {
    return new C();
  }

  @Borrowed("this")
  protected void doStuff1(final int v1, final int v2) {
    this.d.e.f1 = v1;
    // Needs CLock
    this.d.e.f2 = v2;
  }
  
  @Borrowed("this")
  protected void doStuff2(final int v1, final int v2) {
    final D local_d = this.d;
    final E local_e = local_d.e;
    local_e.f1 = v1;
    // Needs CLock
    local_e.f2 = v2;
  }

  @Borrowed("this")
  protected void doStuff3(final int v1, final int v2) {
    final D local_d;
    if (v1 > v2) {
      local_d = D.getD();
    } else {
      local_d = this.d;
    }
    local_d.e.f1 = v1;
    
    /* Here we need lock this.CLock because local_d might cause aggregation
     * into the receiver.
     * 
     * We also need local_d.DLock because local_d might be a random D object
     * that is not aggregated into this C object, in which case the aggregation
     * causes the use of f2 to be reflected as a use of the D_R2 region of the
     * object referenced by local_d.
     */
    local_d.e.f2 = v2;
  }
}
