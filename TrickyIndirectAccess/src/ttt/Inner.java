package ttt;

import com.surelogic.Borrowed;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionEffects;

@Region("public R")
public class Inner {
  @InRegion("R")
  public int f1;
  
  @InRegion("R")
  public int f2;
  
  @Borrowed("this")
  public Inner() {
    // do stuff
  }
  
  @RegionEffects("writes R")
  @Borrowed("this")
  public void setBoth(int a, int b) {
    f1 = a;
    f2 = b;
  }
}
