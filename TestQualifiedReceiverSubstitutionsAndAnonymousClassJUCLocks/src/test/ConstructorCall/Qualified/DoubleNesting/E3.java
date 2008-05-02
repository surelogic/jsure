package test.ConstructorCall.Qualified.DoubleNesting;

import com.surelogic.RequiresLock;

public class E3 extends Outer1.E1 {
  @RequiresLock("o:F1, n:F2")
  public E3(final Outer1 o, final Outer1.Nested1 n, final int y) {
    /* Effects on qualified receiver Outer1.this are mapped to o
     */
    o. super(n, y);
  }
}

