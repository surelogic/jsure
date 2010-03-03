package withEffects;

import com.surelogic.RegionEffects;

public class Child extends Super {
  @RegionEffects("writes withEffects.Super:s")
  protected Child() {
    super();
  }

  @RegionEffects("writes withEffects.Super:s")
  protected Child(int v) {
    super(v);
  }
}
