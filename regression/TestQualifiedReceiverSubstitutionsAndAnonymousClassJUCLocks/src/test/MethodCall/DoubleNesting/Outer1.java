package test.MethodCall.DoubleNesting;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;

@RegionLock("F1 is lockF1 protects f1")
public class Outer1 {
  public final Lock lockF1 = new ReentrantLock();
  public int f1;

  @RegionLock("F2 is lockF2 protects f2")
  public class Nested1 {
    public final Lock lockF2 = new ReentrantLock();
    public int f2;
    
    @RegionLock("F3 is lockF3 protects f3")
    public class Nested2 {
      public final Lock lockF3 = new ReentrantLock();
      public int f3;
      
      @RequiresLock("this:F3, Outer1.this:F1, test.MethodCall.DoubleNesting.Outer1.Nested1.this:F2")
      public int doStuff1() {
        return
          this.f3 + // Assures
          Outer1.this.f1 + // Assures
          Nested1.this.f2; // Assures
      }
      
      public int doStuff2() {
        this.lockF3.lock();
        try {
          Nested1.this.lockF2.lock();
          try {
            Outer1.this.lockF1.lock();
            try {
              return
                this.f3 + // Assures
                Outer1.this.f1 + // Assures
                Nested1.this.f2; // Assures
            } finally {
              Outer1.this.lockF1.unlock();
            }
          } finally {
            Nested1.this.lockF2.unlock();
          } 
        } finally {
          this.lockF3.unlock();
        }
      }

      /**
       * Test case (Special case): receiver is "this", can map the qualified
       * receivers across method contexts.
       */
      @RequiresLock("this:F3, Outer1.this:F1, test.MethodCall.DoubleNesting.Outer1.Nested1.this:F2")
      public int testReceiverIsThis1() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        return this.doStuff1();
      }

      public int testReceiverIsThis2() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        this.lockF3.lock();
        try {
          Nested1.this.lockF2.lock();
          try {
            Outer1.this.lockF1.lock();
            try {
              return this.doStuff1();
            } finally {
              Outer1.this.lockF1.unlock();
            }
          } finally {
            Nested1.this.lockF2.unlock();
          } 
        } finally {
          this.lockF3.unlock();
        }
      }
    }
    
    public class E1 extends Nested2 {
      /**
       * Test case (Special case): receiver is "this", can map the qualified
       * receivers across method contexts.
       */
      @RequiresLock("this:F3, Outer1.this:F1, test.MethodCall.DoubleNesting.Outer1.Nested1.this:F2")
      public int testReceiverIsThis1() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        return this.doStuff1();
      }

      public int testReceiverIsThis2() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        this.lockF3.lock();
        try {
          Nested1.this.lockF2.lock();
          try {
            Outer1.this.lockF1.lock();
            try {
              return this.doStuff1();
            } finally {
              Outer1.this.lockF1.unlock();
            }
          } finally {
            Nested1.this.lockF2.unlock();
          } 
        } finally {
          this.lockF3.unlock();
        }
      }

      /**
       * Test case (Special case): receiver is "super", can map the qualified
       * receivers across method contexts.
       */
      @RequiresLock("this:F3, Outer1.this:F1, test.MethodCall.DoubleNesting.Outer1.Nested1.this:F2")
      public int testReceiverIsSuper1() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        return super.doStuff1();
      }

      public int testReceiverIsSuper2() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        this.lockF3.lock();
        try {
          Nested1.this.lockF2.lock();
          try {
            Outer1.this.lockF1.lock();
            try {
              return super.doStuff1();
            } finally {
              Outer1.this.lockF1.unlock();
            }
          } finally {
            Nested1.this.lockF2.unlock();
          } 
        } finally {
          this.lockF3.unlock();
        }
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
    public int testReceiverIsThis1() {
      /* Effects on qualified receiver Outer1.this are still reported as effects
       * on qualified receiver. But effects on qualified receiver Nested1.this
       * are reported using any instance targets.
       */
      return this.doStuff1(); // F3 and F1 assure, F2 cannot be resolved
    }
    
    public int testReceiverIsThis2() {
      /* Effects on qualified receiver Outer1.this are still reported as effects
       * on qualified receiver. But effects on qualified receiver Nested1.this
       * are reported using any instance targets.
       */
      this.lockF3.lock();
      try {
        Outer1.this.lockF1.lock();
        try {
          return this.doStuff1(); // F3 and F1 assure, F2 cannot be resolved
        } finally {
          Outer1.this.lockF1.unlock();
        }
      } finally {
        this.lockF3.unlock();
      }
    }
    
    /**
     * Test case (Special case): receiver is "super", can map the qualified
     * receivers across method contexts.
     */
    @RequiresLock("this:F3, Outer1.this:F1")
    public int testReceiverIsSuper1() {
      /* Effects on qualified receiver Outer1.this are still reported as effects
       * on qualified receiver. But effects on qualified receiver Nested1.this
       * are reported using any instance targets.
       */
      return super.doStuff1(); // F3 and F1 assure, F2 cannot be resolved
    }

    public int testReceiverIsSuper2() {
      /* Effects on qualified receiver Outer1.this are still reported as effects
       * on qualified receiver. But effects on qualified receiver Nested1.this
       * are reported using any instance targets.
       */
      this.lockF3.lock();
      try {
        Outer1.this.lockF1.lock();
        try {
          return super.doStuff1(); // F3 and F1 assure, F2 cannot be resolved
        } finally {
          Outer1.this.lockF1.unlock();
        }
      } finally {
        this.lockF3.unlock();
      }
    }
  }
  
  @RequiresLock("n1:F3, n2:F3")
  public static int test1(final Nested1.Nested2 n1, final Nested1.Nested2 n2) {
    return
      // Cannot map qualified receivers: use any instance target
      n1.doStuff1() + // F3 assures; F1 and F2 cannot be resolved
      // Cannot map qualified receivers: use any instance target
      n2.doStuff1(); // F3 assures; F1 and F2 cannot be resolved
  }

  public static int test2(final Nested1.Nested2 n1, final Nested1.Nested2 n2) {
    n1.lockF3.lock();
    try {
      n2.lockF3.lock();
      try {
        return
        // Cannot map qualified receivers: use any instance target
        n1.doStuff1() + // F3 assures; F1 and F2 cannot be resolved
        // Cannot map qualified receivers: use any instance target
        n2.doStuff1(); // F3 assures; F1 and F2 cannot be resolved
      } finally {
        n2.lockF3.unlock();
      }
    } finally {
      n1.lockF3.unlock();
    }
  }
}

