package test.ConstructorCall.Unqualified.SingleNesting;

import com.surelogic.RegionEffects;


public class Outer2 extends Outer1 {
  public Outer2() {
    super();
  }

  public class E3 extends Nested {
    @RegionEffects("writes any(Outer1):f1, any(Outer1):f2")
    public E3(final int y) {
      // Effects on qualified receiver turned into any instance effects because there is no lexically enclosing Outer1 type
      super(y);
    }

    @RegionEffects("writes any(Outer1):f1, any(Outer1):f2")
    public E3() {
      // Effects on qualified receiver turned into any instance effects because there is no lexically enclosing Outer1 type
      this(100);
    }
  }
}
