package test.ConstructorCall.Qualified.DoubleNesting;

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
      
      @RegionEffects("writes Outer1.this:f1, test.ConstructorCall.Qualified.DoubleNesting.Outer1.Nested1.this:f2")
      public Nested2(final int z) {
        this.f3 = z;
        Nested1.this.f2 = z + 1;
        Outer1.this.f1 = z + 2;
      }
    }
  }  

  public class E1 extends Nested1.Nested2 {
    @RegionEffects("writes Outer1.this:f1, n:f2")
    public E1(final Nested1 n, final int y) {
      /* Effects on qualified receiver Nested1.this are mapped to effects on n.
       * Effects on qualified receiver Outer1.this are kept as effects on Outer1.this. 
       */
      n. super(y);
    }

    @RegionEffects("writes Outer1.this:f1, oo:f2")
    public E1(final Nested1 oo) {
      /* Effects on qualified receiver Outer1.this are still reported as effects
       * on qualified receiver.
       */
      this(oo, 10);
    }
  }
}

