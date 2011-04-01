package test;

import com.surelogic.Immutable;
import com.surelogic.RegionEffects;

@Immutable
public class Point {
  private final int x;
  private final int y;
  
  
  
  @RegionEffects("none")
  public Point(final int a, final int b) {
    this.x = a;
    this.y = b;
  }
  
  @RegionEffects("none")
  public int getX() {
    return x;
  }
  
  @RegionEffects("none")
  public int getY() {
    return y;
  }
}
