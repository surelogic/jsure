package test.AnonymousClass.SuperIsNotInner;

import com.surelogic.RegionEffects;

public class Super {
  public int f;
  
  public static int count = 0;
  
  @RegionEffects("writes test.AnonymousClass.SuperIsNotInner.Super:count")
  public Super() {
    Super.count += 1;
    this.f = 10;
  }
}
