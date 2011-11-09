package test;

import com.surelogic.Aggregate;
import com.surelogic.Borrowed;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
  @Region("protected OO_R1"),
  @Region("protected OO_R2")
})
public class OO {
  @Unique
  @Aggregate("O_R1 into OO_R1, O_R2 into OO_R2, Instance into Instance")
  protected final O o = new O();
  
  @Borrowed("this")
  @RegionEffects("none")
  public OO() {
    super();
  }
  @Borrowed("this")
  @RegionEffects("writes OO_R1, OO_R2")
  protected void doStuff(final int v1, final int v2) {
    this.o.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes OO_R1, OO_R2")
  protected void doStuff2(final int v1, final int v2) {
    this.o.a.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes OO_R1, OO_R2")
  protected void doStuff3(final int v1, final int v2) {
    this.o.a.b.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes OO_R1, OO_R2")
  protected void doStuff4(final int v1, final int v2) {
    this.o.a.b.c.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes OO_R1, OO_R2")
  protected void doStuff5(final int v1, final int v2) {
    this.o.a.b.c.d.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes OO_R1, OO_R2")
  protected void doStuff6(final int v1, final int v2) {
    this.o.a.b.c.d.e.doStuff(v1, v2);
  }
  
  @Borrowed("this")
  @RegionEffects("writes OO_R1, OO_R2")
  protected void doStuff7(final int v1, final int v2) {
    this.o.a.b.c.d.e.f1 = v1;
    this.o.a.b.c.d.e.f2 = v2;
  }
}
