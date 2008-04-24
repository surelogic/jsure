package test.MethodCall.SingleNesting;

import com.surelogic.RequiresLock;


public class Outer2 extends Outer1 {
  public class E3 extends Nested {
    @RequiresLock("this:F3")
    public int testReceiverIsThis() {
      return this.doStuff(); // F3 assures; F1 and F2 cannot be resolved
    }
    
    @RequiresLock("this:F3")
    public int testReceiverIsSuper() {
      return super.doStuff(); // F3 assures; F1 and F2 cannot be resolved
    }
  }
}
