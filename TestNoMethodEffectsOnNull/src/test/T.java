package test;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public A"),
  @Region("public B"),
  @Region("public C")
})
public class T {
	private int f;



  @RegionEffects("reads a:A, b:B, c:C")
  private static void method(final T a, final T b, final T c) {
    // blah
  }

  @RegionEffects("reads x:A, y:B, z:C")
  public static void test(final T x, final T y, final T z) {
    method(x, y, z);

    method(x, y, null);
    method(x, null, null);
    method(null, null, null);

    method(x, y, (null));
    method(x, (((null))), (T) null);
    method(((T) null), (T) ((T) ((T) null)), z);
  }



  @RegionEffects("writes other:Instance")
  public static void test2(final T other) {
  	other.f = 0;
  	((T) null).f = 10;
  	((T) (T) (T) (null)).f = 100;
  	(((T) ((T) (null)))).f = 1000;
  }
}
