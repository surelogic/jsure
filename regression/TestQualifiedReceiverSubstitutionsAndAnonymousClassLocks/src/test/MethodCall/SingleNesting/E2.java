package test.MethodCall.SingleNesting;

import com.surelogic.RequiresLock;

public class E2 extends Outer1.Nested {
  public E2(final Outer1 sn) {
    sn. super();
  }
  
  @RequiresLock("this:F3")
  public int testReceiverIsThis2() {
    return this.doStuff(); // F3 assures; F1 and F2 cannot be resolved
  }
  
  @RequiresLock("this:F3")
  public int testReceiverIsSuper() {
    return super.doStuff(); // F3 assures; F1 and F2 cannot be resolved
  }
}

