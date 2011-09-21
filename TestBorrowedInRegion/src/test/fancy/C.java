package test.fancy;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;
import com.surelogic.Unique;

@Regions({
  @Region("public R1"),
  @Region("public R2"),
  
  @Region("public static S")
})
public class C {
  @InRegion("R1")
  private int f1;
  
  @InRegion("R2")
  private int f2;
  
  
  
  @Unique("return")
  @RegionEffects("none")
  public C(final int a, final int b) {
    f1 = a;
    f2 = b;
  }
}
