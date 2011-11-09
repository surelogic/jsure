package emptyEffects;

import com.surelogic.RegionEffects;

public class C {
  private int x;



  @RegionEffects("none")
  private void touchNothing() {
  }

  @RegionEffects("none")
  private void callTouchNothing() {
    touchNothing();
  }



  @RegionEffects("none")
  private void touchX() {
    x = 10;
  }

  @RegionEffects("none")
  private void callTouchX() {
    touchX();
  }
}
