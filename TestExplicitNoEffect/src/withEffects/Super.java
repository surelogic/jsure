package withEffects;

import com.surelogic.RegionEffects;

public class Super {
  protected static int s;
  protected static int t;

  @RegionEffects("writes s")
  protected Super() {
    s = 1;
  }

  @RegionEffects("writes s")
  protected Super(int v) {
    s = 1;
    t = v;
  }
}
