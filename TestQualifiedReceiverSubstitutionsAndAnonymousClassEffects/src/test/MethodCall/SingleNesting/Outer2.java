package test.MethodCall.SingleNesting;

import com.surelogic.RegionEffects;


public class Outer2 extends Outer1 {
  public Outer2(final int x) {
    super(x);
  }

  public class E3 extends Nested {
    public E3(final int y) {
      super(y);
    }
    
    @RegionEffects("reads this:f3, any(Outer1):f1, any(Outer1):f2")
    public int testReceiverIsThis() {
      // Effects on qualified receiver turned into any instance effects because there is no lexically enclosing Outer1 type
      return this.doStuff();
    }
    
    @RegionEffects("reads this:f3, any(Outer1):f1, any(Outer1):f2")
    public int testReceiverIsSuper() {
      // Effects on qualified receiver turned into any instance effects because there is no lexically enclosing Outer1 type
      return super.doStuff();
    }
  }
}
