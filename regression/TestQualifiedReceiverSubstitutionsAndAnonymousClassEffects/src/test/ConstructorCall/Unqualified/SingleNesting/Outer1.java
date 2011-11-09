package test.ConstructorCall.Unqualified.SingleNesting;

import com.surelogic.RegionEffects;

public class Outer1 {
  public int f1;
  public int f2;

  public Outer1() {
    f1 = 0;
    f2 = 1;
  }

  public class Nested {
    public int f3;
    
    @RegionEffects("writes Outer1.this:f1, Outer1.this:f2")
    public Nested(final int y) {
      this.f3 = y;
      Outer1.this.f1 = y + 1;
      Outer1.this.f2 = y + 2;
    }
    
    @RegionEffects("writes Outer1.this:f1, Outer1.this:f2")
    public Nested() {
      // Effects on qualified receiver are still reported as effects on qualified receiver
      this(10);
    }
  }
  
  public class E1 extends Nested {
    @RegionEffects("writes Outer1.this:f1, Outer1.this:f2")
    public E1(final int y) {
      // Effects on qualified receiver are still reported as effects on qualified receiver
      super(y);
    }

    @RegionEffects("writes Outer1.this:f1, Outer1.this:f2")
    public E1() {
      // Effects on qualified receiver are still reported as effects on qualified receiver
      this(100);
    }
  }
}

