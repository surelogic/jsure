package test.ConstructorCall.Qualified.DoubleNesting;

import com.surelogic.RequiresLock;

public class E2 extends Outer1.Nested1.Nested2 {
  @RequiresLock("n:F2")
  public E2(final Outer1.Nested1 n, final int y) {
    /* Effects on qualified receiver Nested1.this are mapped to effects on n.
     * Effects on qualified receiver Outer1.this are mapped to any(Outer1)
     */
    n. super(y); // Cannot resolve F1 in calling context
  }
}

