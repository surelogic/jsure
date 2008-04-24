package test.MethodCall.DoubleNesting;

import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;

@RegionLock("F1 is lockF1 protects f1")
public class Outer1 {
  public final Object lockF1 = new Object();
  public int f1;

  @RegionLock("F2 is lockF2 protects f2")
  public class Nested1 {
    public final Object lockF2 = new Object();
    public int f2;
    
    @RegionLock("F3 is lockF3 protects f3")
    public class Nested2 {
      public final Object lockF3 = new Object();
      public int f3;
      
      @RequiresLock("this:F3, Outer1.this:F1, test.MethodCall.DoubleNesting.Outer1.Nested1.this:F2")
      public int doStuff() {
        return
          this.f3 + // Assures
          Outer1.this.f1 + // Assures
          Nested1.this.f2; // Assures
      }

      /**
       * Test case (Special case): receiver is "this", can map the qualified
       * receivers across method contexts.
       */
      @RequiresLock("this:F3, Outer1.this:F1, test.MethodCall.DoubleNesting.Outer1.Nested1.this:F2")
      public int testReceiverIsThis() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        return this.doStuff();
      }
    }
    
    public class E1 extends Nested2 {
      /**
       * Test case (Special case): receiver is "this", can map the qualified
       * receivers across method contexts.
       */
      @RequiresLock("this:F3, Outer1.this:F1, test.MethodCall.DoubleNesting.Outer1.Nested1.this:F2")
      public int testReceiverIsThis2() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        return this.doStuff();
      }
      
      /**
       * Test case (Special case): receiver is "super", can map the qualified
       * receivers across method contexts.
       */
      @RequiresLock("this:F3, Outer1.this:F1, test.MethodCall.DoubleNesting.Outer1.Nested1.this:F2")
      public int testReceiverIsSuper() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        return super.doStuff();
      }
    }
  }  
  
  public class E1 extends Nested1.Nested2 {
    public E1(final Nested1 n) {
      n. super();
    }
    
    /**
     * Test case (Special case): receiver is "this", can map the qualified
     * receivers across method contexts.
     */
    @RequiresLock("this:F3, Outer1.this:F1")
    public int testReceiverIsThis2() {
      /* Effects on qualified receiver Outer1.this are still reported as effects
       * on qualified receiver. But effects on qualified receiver Nested1.this
       * are reported using any instance targets.
       */
      return this.doStuff(); // F3 and F1 assure, F2 cannot be resolved
    }
    
    /**
     * Test case (Special case): receiver is "super", can map the qualified
     * receivers across method contexts.
     */
    @RequiresLock("this:F3, Outer1.this:F1")
    public int testReceiverIsSuper() {
      /* Effects on qualified receiver Outer1.this are still reported as effects
       * on qualified receiver. But effects on qualified receiver Nested1.this
       * are reported using any instance targets.
       */
      return super.doStuff(); // F3 and F1 assure, F2 cannot be resolved
    }
  }
  
  @RequiresLock("n1:F3, n2:F3")
  public static int test(final Nested1.Nested2 n1, final Nested1.Nested2 n2) {
    return
      // Cannot map qualified receivers: use any instance target
      n1.doStuff() + // F3 assures; F1 and F2 cannot be resolved
      // Cannot map qualified receivers: use any instance target
      n2.doStuff(); // F3 assures; F1 and F2 cannot be resolved
  }
}

