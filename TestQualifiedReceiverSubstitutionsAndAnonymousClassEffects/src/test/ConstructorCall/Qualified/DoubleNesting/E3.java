package test.ConstructorCall.Qualified.DoubleNesting;

import com.surelogic.RegionEffects;

public class E3 extends Outer1.E1 {
  @RegionEffects("writes o:f1, n:f2")
  public E3(final Outer1 o, final Outer1.Nested1 n, final int y) {
    /* Effects on qualified receiver Outer1.this are mapped to o
     */
    o. super(n, y);
  }
}

