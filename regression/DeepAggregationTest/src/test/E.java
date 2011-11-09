package test;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;

public class E {
  protected int f1;
  protected int f2;
  
  @Borrowed("this")
  @RegionEffects("none")
  public E() {
    super();
  }
  
  @Borrowed("this")
  @RegionEffects("writes this:f1, this:f2")
  protected void doStuff(final int v1, final int v2) {
    this.f1 = v1;
    this.f2 = v2;
  }
}
