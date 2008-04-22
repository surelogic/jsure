package test.ConstructorCall.Qualified.DoubleNesting;

import com.surelogic.RegionEffects;

public class E3 extends Outer1.E1 {
  @RegionEffects("writes any(Outer1):f1, n:f2")
  public E3(final Outer1 o, final Outer1.Nested1 n, final int y) {
    /* Affects the enclosing Outer1 object of n, which is not known
     * in this context, so we have any instance effect.
     */
    o. super(n, y);
  }
}

