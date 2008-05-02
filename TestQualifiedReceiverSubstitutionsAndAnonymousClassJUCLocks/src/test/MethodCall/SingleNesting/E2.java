package test.MethodCall.SingleNesting;

import com.surelogic.RequiresLock;

public class E2 extends Outer1.Nested {
  public E2(final Outer1 sn) {
    sn. super();
  }
  
  @RequiresLock("this:F3")
  public int testReceiverIsThis1() {
    return this.doStuff(); // F3 assures; F1 and F2 cannot be resolved
  }
  
  public int testReceiverIsThis2() {
    this.lockF3.lock();
    try {
      return this.doStuff(); // F3 assures; F1 and F2 cannot be resolved
    } finally {
      this.lockF3.unlock();
    }
  }
  
  @RequiresLock("this:F3")
  public int testReceiverIsSuper1() {
    return super.doStuff(); // F3 assures; F1 and F2 cannot be resolved
  }
  
  public int testReceiverIsSuper2() {
    this.lockF3.lock();
    try {
      return super.doStuff(); // F3 assures; F1 and F2 cannot be resolved
    } finally {
      this.lockF3.unlock();
    }
  }
}

