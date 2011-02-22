package test.simple;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class Useless {
  @Unique("return")
  public Useless() {
    super();
  }
  
  @Borrowed("this")
  @RegionEffects("reads p:Instance")
  public void twiddleParameter(final Object p) {
    // do nothing
  }

  @Borrowed("this")
  @RegionEffects("reads this:Instance")
  public void twiddleReceiver(final Object p) {
    // do nothing
  }
  
  @Borrowed("this")
  @RegionEffects("reads this:Instance, p:Instance")
  public void twiddleBoth(final Object p) {
    // do nothing
  }
  
  @RegionEffects("reads p2:Instance")
  public static void twiddle1(final @Borrowed Object p1, final Object p2) {
    // do nothing
  }
  
  @RegionEffects("reads p1:Instance")
  public static void twiddle2(final @Borrowed Object p1, final Object p2) {
    // do nothing
  }
  
  @RegionEffects("reads p1:Instance, p2:Instance")
  public static void twiddle3(final @Borrowed Object p1, final Object p2) {
    // do nothing
  }
}
