package test.ConstructorCall.Qualified.SingleNesting;

import com.surelogic.RegionEffects;

public class E2 extends Outer1.Nested {
  @RegionEffects("writes sn:f1, sn:f2")
  public E2(final Outer1 sn, final int y) {
    // Effects on Outer1.this mapped to effects on sn
    sn. super(y);
  }
}

