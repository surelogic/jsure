package test.MethodCall.DoubleNesting;

import com.surelogic.RegionEffects;

public class E2 extends Outer1.Nested1.Nested2 {
  public E2(final Outer1.Nested1 n, final int y) {
    n. super(y);
  }
  
  @RegionEffects("reads this:f3, any(Outer1):f1, any(test.MethodCall.DoubleNesting.Outer1.Nested1):f2")
  public int testReceiverIsThis2() {
    // Effects on qualified receiver turned into any instance effects because there is no lexically enclosing Outer1 type
    return this.doStuff();
  }
  
  @RegionEffects("reads this:f3, any(Outer1):f1, any(test.MethodCall.DoubleNesting.Outer1.Nested1):f2")
  public int testReceiverIsSuper() {
    // Effects on qualified receiver turned into any instance effects because there is no lexically enclosing Outer1 type
    return super.doStuff();
  }
}

