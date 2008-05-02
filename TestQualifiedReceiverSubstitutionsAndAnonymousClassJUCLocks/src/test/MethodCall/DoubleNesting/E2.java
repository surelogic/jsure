package test.MethodCall.DoubleNesting;

import com.surelogic.RequiresLock;

public class E2 extends Outer1.Nested1.Nested2 {
  public E2(final Outer1.Nested1 n) {
    n. super();
  }
  
  @RequiresLock("this:F3")
  public int testReceiverIsThis1() {
    // Effects on qualified receiver turned into any instance effects because there is no lexically enclosing Outer1 type
    return this.doStuff1(); // F3 assures; F1 and F2 cannot be resolved
  }
  
  public int testReceiverIsThis2() {
    this.lockF3.lock();
    try {
      // Effects on qualified receiver turned into any instance effects because there is no lexically enclosing Outer1 type
      return this.doStuff1(); // F3 assures; F1 and F2 cannot be resolved
    } finally {
      this.lockF3.unlock();
    }
  }
  
  @RequiresLock("this:F3")
  public int testReceiverIsSuper1() {
    // Effects on qualified receiver turned into any instance effects because there is no lexically enclosing Outer1 type
    return super.doStuff1(); // F3 assures; F1 and F2 cannot be resolved
  }
  
  @RequiresLock("this:F3")
  public int testReceiverIsSuper2() {
    this.lockF3.lock();
    try {
      // Effects on qualified receiver turned into any instance effects because there is no lexically enclosing Outer1 type
      return super.doStuff1(); // F3 assures; F1 and F2 cannot be resolved
    } finally {
      this.lockF3.unlock();
    }
  }
}

