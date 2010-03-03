package emptyEffects;

import com.surelogic.RegionEffects;

public class Super {
  protected static int z;

  @RegionEffects("none")
  protected Super() {
  }

  @RegionEffects("none")
  protected Super(int v) {
    z = v;
  }
}
