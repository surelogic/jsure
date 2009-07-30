package test;

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

  @Borrowed("this")
  @RegionEffects("writes B_R1, B_R2")
  protected void doStuff(final int v1, final int v2) {
    // Needs BLock because of effects of doStuff
    this.c.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes B_R1, B_R2")
  protected void doStuff2(final int v1, final int v2) {
    // Needs BLock because of effects of doStuff
   this.c.d.doStuff(v1, v2); 
  }
  
  @Borrowed("this")
  @RegionEffects("writes B_R1, B_R2")
  protected void doStuff3(final int v1, final int v2) {
    // Needs BLock because of effects of doStuff
    this.c.d.e.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes B_R1, B_R2")
  protected void doStuff4(final int v1, final int v2) {
   this.c.d.e.f1 = v1;
   // Needs BLock because of direct use of f2
   this.c.d.e.f2 = v2;
  }
}
