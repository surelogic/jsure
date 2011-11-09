package test.MethodCall.DoubleNesting;

import com.surelogic.RequiresLock;

public class E2 extends Outer1.Nested1.Nested2 {
  public E2(final Outer1.Nested1 n) {
    n. super();
  }
  
  @RequiresLock("this:F3")
  public int testReceiverIsThis2() {
    // Effects on qualified receiver turned into any instance effects because there is no lexically enclosing Outer1 type
    return this.doStuff(); // F3 assures; F1 and F2 cannot be resolved
  }
  
  @RequiresLock("this:F3")
  public int testReceiverIsSuper() {
    // Effects on qualified receiver turned into any instance effects because there is no lexically enclosing Outer1 type
    return super.doStuff(); // F3 assures; F1 and F2 cannot be resolved
  }
}

