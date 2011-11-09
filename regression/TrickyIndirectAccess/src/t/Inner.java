package t;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;

public class Inner {
  public int f1;
  public int f2;
  
  @Borrowed("this")
  public Inner() {
    // do stuff
  }
  
  @RegionEffects("writes Instance")
  @Borrowed("this")
  public void setBoth(int a, int b) {
    f1 = a;
    f2 = b;
  }
}
