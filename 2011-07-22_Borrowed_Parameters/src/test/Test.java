package test;

import com.surelogic.Borrowed;
import com.surelogic.RegionEffects;
import com.surelogic.Unique;

public class Test {
  private static void bad(final Object o) {}
  
  @RegionEffects("none")
  public static void borrowIt1(final @Borrowed Object p) {
    borrowIt2(p);
  }
  
  @RegionEffects("none")
  public static  void borrowIt2(final @Borrowed Object p) {
    bad(p);
  }
  
  @RegionEffects("none")
  public static void borrow(final @Borrowed Object p) {
    borrowIt1(p);
    borrowIt2(p);
  }
  
  
  
  @Unique("return")
  public Test() {
    super();
    borrowIt1(this);
  }
  
  @Borrowed("this")
  public Test(final int x) {
    borrowIt1(this);
  }
  
  @Borrowed("this")
  public void m(final boolean f, final @Borrowed Object p) {
    borrowIt1(f ? this : p);
  }
}

