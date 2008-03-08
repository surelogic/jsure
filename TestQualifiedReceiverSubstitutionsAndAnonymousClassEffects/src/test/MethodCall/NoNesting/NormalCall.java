package test.MethodCall.NoNesting;

import com.surelogic.RegionEffects;

public class NormalCall {
  public int f1;
  public int f2;
  
  public NormalCall(final int x) {
    f1 = x;
    f2 = x + 1;
  }
  
  // Test that "NormalCall.this" is treated the same as "this"
  @RegionEffects("reads NormalCall.this:f1, this:f2")
  public int doStuff() {
    return
      NormalCall.this.f1 +
      f2;
  }

  @RegionEffects("reads a:f1, a:f2, b:f1, b:f2")
  public static int test(final NormalCall a, final NormalCall b) {
    return
      // Maps this -> a (this should be the same as NormalCall.this)
      a.doStuff() +
      // Maps this -> b
      b.doStuff();
  }
}
