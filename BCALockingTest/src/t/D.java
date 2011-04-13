package t;

import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;
import com.surelogic.UniqueInRegion;

@Regions({
  @Region("protected D_R1"),
  @Region("protected D_R2"),
  @Region("protected OTHER")
})
@RegionLocks({
  @RegionLock("DLock is this protects D_R2"),
  @RegionLock("OLock is this protects OTHER")
})
public class D {
  @UniqueInRegion("f1 into D_R1, f2 into D_R2, Instance into Instance")
  protected final E e = new E();

  @UniqueInRegion("Instance into OTHER")
  protected final E e2 = new E();
  
  @Borrowed("this")
  @RegionEffects("none")
  public D() {
    super();
  }
  
  public static D getD() {
    return new D();
  }
  
  @Borrowed("this")
  protected void doStuff1(final int v1, final int v2) {
    this.e.f1 = v1;
    // Needs DLock
    this.e.f2 = v2;
  }
  
  
  @Borrowed("this")
  protected void doStuff2(final int v1, final int v2) {
    final E local_e = this.e;
    local_e.f1 = v1;
    // Needs DLock
    local_e.f2 = v2;
  }
  
  @Borrowed("this")
  protected void doStuff3(final int v1, final int v2) {
    final E local_e;
    if (v1 > v2) {
      local_e = E.getE();
    } else {
      local_e = this.e;
    }
    local_e.f1 = v1;
    
    /* Here we need lock this.DLock because local_e might cause aggregation
     * into the receiver.
     */
    local_e.f2 = v2;
  }
  
  @Borrowed("this")
  protected void doStuff4(final int v1, final int v2) {
    final E local_e;
    if (v1 > v2) {
      local_e = this.e2;
    } else {
      local_e = this.e;
    }
    /* Here we need OLock because local_e could refer to the object referenced
     * by this.e2.
     */
    local_e.f1 = v1;
    
    /* Here we need both the locks DLock and OLock because local_e could refer
     * to the object referenced by either 'e' or 'e2', and thus we could be aggregated
     * into either D_R2 or OTHER.
     */
    local_e.f2 = v2;
  }
}
