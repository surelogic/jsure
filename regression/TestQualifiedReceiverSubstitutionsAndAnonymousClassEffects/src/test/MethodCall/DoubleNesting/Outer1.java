package test.MethodCall.DoubleNesting;

import com.surelogic.RegionEffects;

public class Outer1 {
  public int f1;

  public Outer1(final int x) {
    f1 = x;
  }

  public class Nested1 {
    public int f2;
    
    public Nested1(final int y) {
      f2 = y;
    }
    
    public class Nested2 {
      public int f3;
      
      public Nested2(final int z) {
        f3 = z;
      }

      @RegionEffects("reads this:f3, Outer1.this:f1, test.MethodCall.DoubleNesting.Outer1.Nested1.this:f2")
      public int doStuff() {
        return
          this.f3 +
          Outer1.this.f1 +
          Nested1.this.f2;
      }

      /**
       * Test case (Special case): receiver is "this", can map the qualified
       * receivers across method contexts.
       */
      @RegionEffects("reads this:f3, Outer1.this:f1, test.MethodCall.DoubleNesting.Outer1.Nested1.this:f2")
      public int testReceiverIsThis() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        return this.doStuff();
      }
    }
    
    public class E1 extends Nested2 {
      public E1(final int y) {
        super(y);
      }
      
      /**
       * Test case (Special case): receiver is "this", can map the qualified
       * receivers across method contexts.
       */
      @RegionEffects("reads this:f3, Outer1.this:f1, test.MethodCall.DoubleNesting.Outer1.Nested1.this:f2")
      public int testReceiverIsThis2() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        return this.doStuff();
      }
      
      /**
       * Test case (Special case): receiver is "super", can map the qualified
       * receivers across method contexts.
       */
      @RegionEffects("reads this:f3, Outer1.this:f1, test.MethodCall.DoubleNesting.Outer1.Nested1.this:f2")
      public int testReceiverIsSuper() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        return super.doStuff();
      }
    }
  }  
  
  public class E1 extends Nested1.Nested2 {
    public E1(final Nested1 n, final int y) {
      n. super(y);
    }
    
    /**
     * Test case (Special case): receiver is "this", can map the qualified
     * receivers across method contexts.
     */
    @RegionEffects("reads this:f3, Outer1.this:f1, any(test.MethodCall.DoubleNesting.Outer1.Nested1):f2")
    public int testReceiverIsThis2() {
      /* Effects on qualified receiver Outer1.this are still reported as effects
       * on qualified receiver. But effects on qualified receiver Nested1.this
       * are reported using any instance targets.
       */
      return this.doStuff();
    }
    
    /**
     * Test case (Special case): receiver is "super", can map the qualified
     * receivers across method contexts.
     */
    @RegionEffects("reads this:f3, Outer1.this:f1, any(test.MethodCall.DoubleNesting.Outer1.Nested1):f2")
    public int testReceiverIsSuper() {
      /* Effects on qualified receiver Outer1.this are still reported as effects
       * on qualified receiver. But effects on qualified receiver Nested1.this
       * are reported using any instance targets.
       */
      return super.doStuff();
    }
  }
  
  @RegionEffects("reads n1:f3, n2:f3, any(Outer1):f1, any(test.MethodCall.DoubleNesting.Outer1.Nested1):f2")
  public static int test(final Nested1.Nested2 n1, final Nested1.Nested2 n2) {
    return
      // Cannot map qualified receivers: use any instance target
      n1.doStuff() +
      // Cannot map qualified receivers: use any instance target
      n2.doStuff();
  }
}

