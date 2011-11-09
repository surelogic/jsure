package test.ConstructorCall.Qualified.SingleNesting;

import com.surelogic.Borrowed;
import com.surelogic.RequiresLock;

public class E2 extends Outer1.Nested {
  @RequiresLock("sn:F1, sn:F2")
  @Borrowed("this")
  public E2(final Outer1 sn, final int y) {
    // Effects on Outer1.this mapped to effects on sn
    sn. super(y);
  }
}

