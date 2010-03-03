package withEffects;

import com.surelogic.RegionEffects;

public class C {
  private int f;
  private int g;



  @RegionEffects("writes f")
  private void touchF() {
    f = 1;
  }

  @RegionEffects("writes f")
  private void callTouchF() {
    touchF();
  }



  @RegionEffects("writes f")
  private void touchFandG() {
    f = 1;
    g = 2;
  }

  @RegionEffects("writes f")
  private void callTouchFandG() {
    touchFandG();
  }
}
