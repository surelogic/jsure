package test.ConstructorCall.Qualified.SingleNesting;

import com.surelogic.Borrowed;
import com.surelogic.RequiresLock;
import com.surelogic.SingleThreaded;

public class E2 extends Outer1.Nested {
  @RequiresLock("sn:F1, sn:F2")
  @SingleThreaded
  @Borrowed("this")
  public E2(final Outer1 sn, final int y) {
    // Effects on Outer1.this mapped to effects on sn
    sn. super(y);
  }
}

