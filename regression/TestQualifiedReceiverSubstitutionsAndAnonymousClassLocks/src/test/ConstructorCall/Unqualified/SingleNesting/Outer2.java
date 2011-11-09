package test.ConstructorCall.Unqualified.SingleNesting;

import com.surelogic.Borrowed;


public class Outer2 extends Outer1 {
  @Borrowed("this")
  public Outer2() {
    super();
  }

  public class E3 extends Nested {
    @Borrowed("this")
    public E3(final int y) {
      // Effects on qualified receiver turned into any instance effects because there is no lexically enclosing Outer1 type
      super(y); // Cannot resolve F1 or F2
    }

    @Borrowed("this")
    public E3() {
      // Effects on qualified receiver turned into any instance effects because there is no lexically enclosing Outer1 type
      this(100);  // Constructor "this(int)" is already broken, so it cannot declare the locks it needs
    }
  }
}
