package test;

import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.Regions;
import com.surelogic.UniqueInRegion;

@Regions({
  @Region("protected D_R1"),
  @Region("protected D_R2")
})
@RegionLock("DLock is this protects D_R2")
public class D {
  @UniqueInRegion("f1 into D_R1, f2 into D_R2, Instance into Instance")
  protected final E e = new E();

  @Borrowed("this")
  @RegionEffects("none")
  public D() {
    super();
  }

  @Borrowed("this")
  @RegionEffects("writes D_R1, D_R2")
  protected void doStuff(final int v1, final int v2) {
    // Needs DLock because of the effects of doStuff()
    this.e.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes D_R1, D_R2")
  protected void doStuff2(final int v1, final int v2) {
    this.e.f1 = v1;
    // Needs DLOck because of direct use of f2
    this.e.f2 = v2;
  }
}
