package test;

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
  @RegionEffects("writes A_R1, A_R2")
  protected void doStuff(final int v1, final int v2) {
    // Needs ALock because of effects of doStuff
    this.b.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes A_R1, A_R2")
  protected void doStuff2(final int v1, final int v2) {
    // Needs ALock because of effects of doStuff
    this.b.c.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes A_R1, A_R2")
  protected void doStuff3(final int v1, final int v2) {
    // Needs ALock because of effects of doStuff
    // Uniqueness fails here, because of the primordial node
    this.b.c.d.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes A_R1, A_R2")
  protected void doStuff4(final int v1, final int v2) {
    // Needs ALock because of effects of doStuff
    this.b.c.d.e.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes A_R1, A_R2")
  protected void doStuff5(final int v1, final int v2) {
    this.b.c.d.e.f1 = v1;
    // Needs ALock because of direct use of f2
    this.b.c.d.e.f2 = v2;
  }
}
