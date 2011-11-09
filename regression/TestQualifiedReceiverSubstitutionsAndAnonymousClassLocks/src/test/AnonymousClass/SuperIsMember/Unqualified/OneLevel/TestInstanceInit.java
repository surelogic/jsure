package test.AnonymousClass.SuperIsMember.Unqualified.OneLevel;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;

@RegionLocks({
  @RegionLock("T1 is lockT1 protects t1"),
  @RegionLock("T2 is lockT2 protects t2")
})
public class TestInstanceInit {
  public final Object lockT1 = new Object();
  public final Object lockT2 = new Object();
  public int t1;
  public int t2;
  
  
  
  @RegionLock("F is lockF protects f")
  public class Super {
    public final Object lockF = new Object();
    public int f;
    
    @Borrowed("this")
    @RequiresLock("TestInstanceInit.this:T1")
    public Super() {
      // do stuff
      TestInstanceInit.this.t1 = 5;
      this.f = 10;
    }
  }

  
  
  {
    /* The immediately enclosing instance of s1 is "this" (a Test object)
     * The immediately enclosing instance with respect to Super is this
     * 
     * Writes this.t1, this.t2
     */
    synchronized (lockT1) {
      synchronized (lockT2) {
        final Super s1  = new Super() { 
          private int g = 10;
          { t2 += 1; }
        };
      }
    }
  }
  
  public TestInstanceInit() {
    // do nothing, but initializer for s1 is analyzed
  }


  
  @RegionLock("M1 is lockM1 protects m1")
  public class Middle1 {
    public final Object lockM1 = new Object();
    public int m1;
    
    {
      /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
       * The immediately enclosing instance with respect to Super is TestInstanceInit.this
       * 
       * Writes TestInstanceInit.this.t1, TestInstanceInit.this.t2, this.m1
       */
      synchronized (lockM1) {
        final Super s3 = new Super() { 
          private int g = 10;
          { m1 = 10; }
          { t2 += 1; }
        };      
      }
    }
    
    @RequiresLock("TestInstanceInit.this:T1, TestInstanceInit.this:T2")
    public Middle1() {
      // do nothing, but initializer for s3 is analyzed
    }

  
  
    @RegionLock("M2 is lockM2 protects m2")
    public class Middle2 {
      public final Object lockM2 = new Object();
      public int m2;

      {
        /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
         * The immediately enclosing instance with respect to Super is TestInstanceInit.this
         * 
         * Writes TestInstanceInit.this.t1, TestInstanceInit.this.t1, Middle1.this.m1, this.m2
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
      
      @RequiresLock("TestInstanceInit.this:T1, TestInstanceInit.this:T2, test.AnonymousClass.SuperIsMember.Unqualified.OneLevel.TestInstanceInit.Middle1.this:M1")
      public Middle2() {
        // do nothing, but initializer for s5 is analyzed
      }
    }
  }
}
