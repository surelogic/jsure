package test;

import com.surelogic.Aggregate;
import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
  @Region("protected O_R1"),
  @Region("protected O_R2")
})
public class O {
  @Unique
  @Aggregate("A_R1 into O_R1, A_R2 into O_R2, Instance into Instance")
  protected final A a = new A();
  
  @Borrowed("this")
  @RegionEffects("none")
  public O() {
    super();
  }

  @Borrowed("this")
  @RegionEffects("writes O_R1, O_R2")
  protected void doStuff(final int v1, final int v2) {
    this.a.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes O_R1, O_R2")
  protected void doStuff2(final int v1, final int v2) {
    this.a.b.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes O_R1, O_R2")
  protected void doStuff3(final int v1, final int v2) {
    this.a.b.c.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes O_R1, O_R2")
  protected void doStuff4(final int v1, final int v2) {
    this.a.b.c.d.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes O_R1, O_R2")
  protected void doStuff5(final int v1, final int v2) {
    this.a.b.c.d.e.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes O_R1, O_R2")
  protected void doStuff6(final int v1, final int v2) {
    this.a.b.c.d.e.f1 = v1;
    this.a.b.c.d.e.f2 = v2;
  }
}
