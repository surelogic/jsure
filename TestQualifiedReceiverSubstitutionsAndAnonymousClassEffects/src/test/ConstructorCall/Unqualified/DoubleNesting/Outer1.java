package test.ConstructorCall.Unqualified.DoubleNesting;

import com.surelogic.RegionEffects;

public class Outer1 {
  public int f1;

  public Outer1() {
    f1 = 0;
  }

  public class Nested1 {
    public int f2;
    
    public Nested1() {
      f2 = 1;
    }
    
    public class Nested2 {
      public int f3;
      
      @RegionEffects("writes Outer1.this:f1, test.ConstructorCall.Unqualified.DoubleNesting.Outer1.Nested1.this:f2")
      public Nested2(final int z) {
        this.f3 = z;
        Nested1.this.f2 = z + 1;
        Outer1.this.f1 = z + 2;
      }

      @RegionEffects("writes Outer1.this:f1, test.ConstructorCall.Unqualified.DoubleNesting.Outer1.Nested1.this:f2")
      public Nested2() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        this(1);
      }
    }
    
    public class E1 extends Nested2 {
      @RegionEffects("writes Outer1.this:f1, test.ConstructorCall.Unqualified.DoubleNesting.Outer1.Nested1.this:f2")
      public E1(final int y) {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        super(y);
      }

      @RegionEffects("writes Outer1.this:f1, test.ConstructorCall.Unqualified.DoubleNesting.Outer1.Nested1.this:f2")
      public E1() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        this(10);
      }
    }
  }  
}

