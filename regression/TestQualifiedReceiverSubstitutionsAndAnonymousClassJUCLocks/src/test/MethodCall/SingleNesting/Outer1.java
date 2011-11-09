package test.MethodCall.SingleNesting;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;

@RegionLocks({
  @RegionLock("F1 is lockF1 protects f1"),
  @RegionLock("F2 is lockF2 protects f2")
})
public class Outer1 {
  public final Lock lockF1 = new ReentrantLock();
  public final Lock lockF2 = new ReentrantLock();
  public int f1;
  public int f2;

  @RegionLock("F3 is lockF3 protects f3")
  public class Nested {
    public final Lock lockF3 = new ReentrantLock();
    public int f3;
    
    @RequiresLock("this:F3, Outer1.this:F1, Outer1.this:F2")
    public int doStuff() {
      return
        this.f3 + // Assures
        Outer1.this.f1 + // Assures
        Outer1.this.f2; // Assures
    }
    
    @RequiresLock("this:F3, Outer1.this:F1, Outer1.this:F2")
    public int testReceiverIsThis1() {
      // Use of qualified receiver is still reported as qualified receiver
      return this.doStuff(); // All three locks assure
    }

    public int testReceiverIsThis2() {
      // Use of qualified receiver is still reported as qualified receiver
      Outer1.this.lockF1.lock();
      try {
        Outer1.this.lockF2.lock();
        try {
          this.lockF3.lock();
          try {
            return this.doStuff(); // All three locks assure
          } finally {
            this.lockF3.unlock();
          }
        } finally {
          Outer1.this.lockF2.unlock();
        }
      } finally {
        Outer1.this.lockF1.unlock();
      }
    }
  }
  
  public class E1 extends Nested {
    @RequiresLock("this:F3, Outer1.this:F1, Outer1.this:F2")
    public int testReceiverIsThis1() {
      // Use of qualified receiver is still reported as qualified receiver
      return this.doStuff(); // All three locks assure
    }
    
    public int testReceiverIsThis2() {
      // Use of qualified receiver is still reported as qualified receiver
      Outer1.this.lockF1.lock();
      try {
        Outer1.this.lockF2.lock();
        try {
          this.lockF3.lock();
          try {
            return this.doStuff(); // All three locks assure
          } finally {
            this.lockF3.unlock();
          }
        } finally {
          Outer1.this.lockF2.unlock();
        }
      } finally {
        Outer1.this.lockF1.unlock();
      }
    }

    @RequiresLock("this:F3, Outer1.this:F1, Outer1.this:F2")
    public int testReceiverIsSuper1() {
      // Use of qualified receiver is still reported as qualified receiver
      return super.doStuff(); // All three locks assure
    }

    public int testReceiverIsSuper2() {
      // Use of qualified receiver is still reported as qualified receiver
      Outer1.this.lockF1.lock();
      try {
        Outer1.this.lockF2.lock();
        try {
          this.lockF3.lock();
          try {
            return super.doStuff(); // All three locks assure
          } finally {
            this.lockF3.unlock();
          }
        } finally {
          Outer1.this.lockF2.unlock();
        }
      } finally {
        Outer1.this.lockF1.unlock();
      }
    }
  }
  
  
  @RequiresLock("n1:F3, n2:F3")
  public static int test1(final Nested n1, final Nested n2) {
    return
      // Cannot map qualified receiver
      n1.doStuff() + // F3 assures, F1 and F2 cannot be resolved
      // Cannot map qualified receiver
      n2.doStuff(); // F3 assures, F1 and F2 cannot be resolved
  }

  public static int test2(final Nested n1, final Nested n2) {
    n1.lockF3.lock();
    try {
      n2.lockF3.lock();
      try {
        return
        // Cannot map qualified receiver
        n1.doStuff() + // F3 assures, F1 and F2 cannot be resolved
        // Cannot map qualified receiver
        n2.doStuff(); // F3 assures, F1 and F2 cannot be resolved
      } finally {
        n2.lockF3.unlock();
      }
    } finally {
      n1.lockF3.unlock();
    }
  }
}

