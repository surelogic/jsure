package test.ConstructorCall.Unqualified.DoubleNesting;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.SingleThreaded;


@RegionLock("F1 is lockF1 protects f1")
public class Outer1 {
  public final Lock lockF1 = new ReentrantLock();
  public int f1;

  @SingleThreaded
  @Borrowed("this")
  public Outer1() {
    f1 = 0;
  }

  @RegionLock("F2 is lockF2 protects f2")
  public class Nested1 {
    public final Lock lockF2 = new ReentrantLock();
    public int f2;
    
    @SingleThreaded
    @Borrowed("this")
    public Nested1() {
      f2 = 1;
    }
    
    @RegionLock("F3 is lockF3 protects f3")
    public class Nested2 {
      public final Lock lockF3 = new ReentrantLock();
      public int f3;
      
      @RequiresLock("Outer1.this:F1, test.ConstructorCall.Unqualified.DoubleNesting.Outer1.Nested1.this:F2")
      @SingleThreaded
      @Borrowed("this")
      public Nested2(final int z) {
        this.f3 = z;
        Nested1.this.f2 = z + 1;
        Outer1.this.f1 = z + 2;
      }

      public Nested2(final int z, final float other) {
        Outer1.this.lockF1.lock();
        try {
          Nested1.this.lockF2.lock();
          try {
            this.lockF3.lock();
            try {
              this.f3 = z;
              Nested1.this.f2 = z + 1;
              Outer1.this.f1 = z + 2;
            } finally {
              this.lockF3.unlock();
            }
          } finally {
            Nested1.this.lockF2.unlock();
          }
        } finally {
          Outer1.this.lockF1.unlock();
        }
      }

      @RequiresLock("Outer1.this:F1, test.ConstructorCall.Unqualified.DoubleNesting.Outer1.Nested1.this:F2")
      @SingleThreaded
      @Borrowed("this")
      public Nested2() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        this(1);
      }
    }
    
    public class E1 extends Nested2 {
      @RequiresLock("Outer1.this:F1, test.ConstructorCall.Unqualified.DoubleNesting.Outer1.Nested1.this:F2")
      @SingleThreaded
      @Borrowed("this")
      public E1(final int y) {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        super(y);
      }

      @RequiresLock("Outer1.this:F1, test.ConstructorCall.Unqualified.DoubleNesting.Outer1.Nested1.this:F2")
      @SingleThreaded
      @Borrowed("this")
      public E1() {
        // Effects on qualified receiver are still reported as effects on qualified receiver
        this(10);
      }
    }
  }  
}

