package emptyEffects;

import com.surelogic.RegionEffects;

public class Child extends Super {
  @RegionEffects("none")
  protected Child() {
    super();
  }

  @RegionEffects("none")
  protected Child(int v) {
    super(v);
  }
}
