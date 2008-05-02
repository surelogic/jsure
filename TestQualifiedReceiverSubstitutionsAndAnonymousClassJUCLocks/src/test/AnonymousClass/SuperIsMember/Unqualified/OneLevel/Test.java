package test.AnonymousClass.SuperIsMember.Unqualified.OneLevel;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;
import com.surelogic.SingleThreaded;

@RegionLocks({
  @RegionLock("T1 is lockT1 protects t1"),
  @RegionLock("T2 is lockT2 protects t2")
})
public class Test {
  public final Lock lockT1 = new ReentrantLock();
  public final Lock lockT2 = new ReentrantLock();
  public int t1;
  public int t2;
  
  
  
  @RegionLock("F is lockF protects f")
  public class Super {
    public final Lock lockF = new ReentrantLock();
    public int f;
    
    @SingleThreaded
    @Borrowed("this")
    @RequiresLock("Test.this:T1")
    public Super() {
      // do stuff
      Test.this.t1 = 5;
      this.f = 10;
    }
  }

  
  
  public Test() {
    /* The immediately enclosing instance of s1 is "this" (a Test object)
     * The immediately enclosing instance with respect to Super is Test.this.
     * 
     * Writes this.t1, this.t2
     */
    lockT1.lock();
    try {
      lockT2.lock();
      try {
        final Super s1 = new Super() { 
          private int g = 10;
          { t2 += 1; }
        };
      } finally {
        lockT2.unlock();
      }
    } finally {
      lockT1.unlock();
    }
  }

  @RequiresLock("T1, T2")
  public void stuff() {
    /* The immediately enclosing instance of s2 is "this" (a Test object)
     * The immediately enclosing instance with respect to Super is Test.this.
     * 
     * Writes this.t1, this.t2
     */
    final Super s2 = new Super() { 
      private int g = 10;
      { t2 += 1; }
    };
  }

  
  
  @RegionLock("M1 is lockM1 protects m1")
  public class Middle1 {
    public final Lock lockM1 = new ReentrantLock();
    public int m1;
    
    @RequiresLock("Test.this:T1, Test.this:T2")
    public Middle1() {
      /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
       * The immediately enclosing instance with respect to Super is Test.this.
       * 
       * Writes Test.this.t1, Test.this.t2, this.m1
       */
      lockM1.lock();
      try {
        final Super s3 = new Super() { 
          private int g = 10;
          { m1 = 10; }
          { t2 += 1; }
        };
      } finally {
        lockM1.unlock();
      }
    }

    @RequiresLock("Test.this:T1, Test.this:T2, this:M1")
    public void stuff1() {
      /* The immediately enclosing instance of s4 is "this" (a Middle1 object)
       * The immediately enclosing instance with respect to Super is Test.this.
       * 
       * Writes Test.this.t1, Test.this.t2, this.m1
       */
      final Super s4 = new Super() { 
        private int g = 10;
        { m1 = 20; }
        { t2 += 1; }
      };
    }

    public void stuff2() {
      lockT1.lock();
      try {
        lockT2.lock();
        try {
          lockM1.lock();
          try {
            /* The immediately enclosing instance of s4 is "this" (a Middle1 object)
             * The immediately enclosing instance with respect to Super is Test.this.
             * 
             * Writes Test.this.t1, Test.this.t2, this.m1
             */
            final Super s4 = new Super() { 
              private int g = 10;
              { m1 = 20; }
              { t2 += 1; }
            };
          } finally {
            lockM1.unlock();
          }
        } finally {
          lockT2.unlock();
        }
      } finally {
        lockT1.unlock();
      }
    }

  
  
    @RegionLock("M2 is lockM2 protects m2")
    public class Middle2 {
      public final Lock lockM2 = new ReentrantLock();
      public int m2;
      
      @RequiresLock("Test.this:T1, Test.this:T2, test.AnonymousClass.SuperIsMember.Unqualified.OneLevel.Test.Middle1.this:M1")
      public Middle2() {
        /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
         * The immediately enclosing instance with respect to Super is Test.this.
         * 
         * Writes Test.this.t1, Test.this.t2, Middle1.this.m1, this.m2
         */
        lockM2.lock();
        try {
          final Super s5 = new Super() { 
            private int g = 10;
            { m1 = 10; }
            { m2 = 20; }
            { t2 += 1; }
          };
        } finally {
          lockM2.unlock();
        }
      }

      @RequiresLock("Test.this:T1, Test.this:T2, test.AnonymousClass.SuperIsMember.Unqualified.OneLevel.Test.Middle1.this:M1, this:M2")
      public void stuff1() {
        /* The immediately enclosing instance of s6 is "this" (a Middle2 object)
         * The immediately enclosing instance with respect to Super is Test.this.
         * 
         * Writes Test.this.t1, Test.this.t2, Middle.this.m1, this.m2
         */
        final Super s6 = new Super() { 
          private int g = 10;
          { m1 = 20; }
          { m2 = 30; }
          { t2 += 1; }
        };
      }

      public void stuff2() {
        lockT1.lock();
        try {
          lockT2.lock();
          try {
            lockM1.lock();
            try {
              lockM2.lock();
              try {
                /* The immediately enclosing instance of s6 is "this" (a Middle2 object)
                 * The immediately enclosing instance with respect to Super is Test.this.
                 * 
                 * Writes Test.this.t1, Test.this.t2, Middle.this.m1, this.m2
                 */
                final Super s6 = new Super() { 
                  private int g = 10;
                  { m1 = 20; }
                  { m2 = 30; }
                  { t2 += 1; }
                };
              } finally {
                lockM2.unlock();
              }
            } finally {
              lockM1.unlock();
            }
          } finally {
            lockT2.unlock();
          }
        } finally {
          lockT1.unlock();
        }
      }
    }
  }
}
