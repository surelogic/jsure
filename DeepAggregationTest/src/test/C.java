package test;

import com.surelogic.Aggregate;
import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
  @Region("protected C_R1"),
  @Region("protected C_R2")
})
@RegionLock("CLock is this protects C_R2")
public class C {
  @Unique
  @Aggregate("D_R1 into C_R1, D_R2 into C_R2, Instance into Instance")
  protected final D d = new D();
  
  @Borrowed("this")
  @RegionEffects("none")
  public C() {
    super();
  }

  @Borrowed("this")
  @RegionEffects("writes C_R1, C_R2")
  protected void doStuff(final int v1, final int v2) {
    // Needs CLock because of effects of doStuff
    this.d.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes C_R1, C_R2")
  protected void doStuff2(final int v1, final int v2) {
    // Needs CLock because of effects of doStuff
    this.d.e.doStuff(v1, v2);
  }

  @Borrowed("this")
  @RegionEffects("writes C_R1, C_R2")
  protected void doStuff3(final int v1, final int v2) {
    this.d.e.f1 = v1;
    // Needs CLock because of direct use of f2
    this.d.e.f2 = v2;
  }
}
