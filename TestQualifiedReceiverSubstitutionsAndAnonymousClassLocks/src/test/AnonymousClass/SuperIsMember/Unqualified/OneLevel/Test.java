package test.AnonymousClass.SuperIsMember.Unqualified.OneLevel;

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
  public final Object lockT1 = new Object();
  public final Object lockT2 = new Object();
  public int t1;
  public int t2;
  
  
  
  @RegionLock("F is lockF protects f")
  public class Super {
    public final Object lockF = new Object();
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
    synchronized (lockT1) {
      synchronized (lockT2) {
        final Super s1 = new Super() { 
          private int g = 10;
          { t2 += 1; }
        };
      }
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
    public final Object lockM1 = new Object();
    public int m1;
    
    @RequiresLock("Test.this:T1, Test.this:T2")
    public Middle1() {
      /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
       * The immediately enclosing instance with respect to Super is Test.this.
       * 
       * Writes Test.this.t1, Test.this.t2, this.m1
       */
      synchronized (lockM1) {
        final Super s3 = new Super() { 
          private int g = 10;
          { m1 = 10; }
          { t2 += 1; }
        };
      }
    }

    @RequiresLock("Test.this:T1, Test.this:T2, this:M1")
    public void stuff() {
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

  
  
    @RegionLock("M2 is lockM2 protects m2")
    public class Middle2 {
      public final Object lockM2 = new Object();
      public int m2;
      
      @RequiresLock("Test.this:T1, Test.this:T2, test.AnonymousClass.SuperIsMember.Unqualified.OneLevel.Test.Middle1.this:M1")
      public Middle2() {
        /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
         * The immediately enclosing instance with respect to Super is Test.this.
         * 
         * Writes Test.this.t1, Test.this.t2, Middle1.this.m1, this.m2
         */
        synchronized (lockM2) {
          final Super s5 = new Super() { 
            private int g = 10;
            { m1 = 10; }
            { m2 = 20; }
            { t2 += 1; }
          };
        }
      }

      @RequiresLock("Test.this:T1, Test.this:T2, test.AnonymousClass.SuperIsMember.Unqualified.OneLevel.Test.Middle1.this:M1, this:M2")
      public void stuff() {
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
    }
  }
}
