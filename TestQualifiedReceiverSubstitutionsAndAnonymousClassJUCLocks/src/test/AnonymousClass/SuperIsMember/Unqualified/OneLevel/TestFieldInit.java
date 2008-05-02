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
public class TestFieldInit {
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
    @RequiresLock("TestFieldInit.this:T1")
    public Super() {
      // do stuff
      TestFieldInit.this.t1 = 5;
      this.f = 10;
    }
  }

  
  
  
  /* The immediately enclosing instance of s1 is "this" (a Test object)
   * The immediately enclosing instance with respect to Super is this
   * 
   * Writes this.t1, this.t2
   */
  final Super s1  = new Super() { // No way to hold T1 
    private int g = 10;
    { t2 += 1; } // No way to hold T2
  };
  
  public TestFieldInit() {
    /* do nothing, but initializer for s1 is analyzed.
     * Writes this.t1, this.t2, both of which are masked.
     */
  }



  @RegionLock("M1 is lockM1 protects m1")
  public class Middle1 {
    public final Lock lockM1 = new ReentrantLock();
    public int m1;
    
    /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
     * The immediately enclosing instance with respect to Super is TestFieldInit.this
     * 
     * Writes TestFieldInit.this.t1, TestFieldInit.this.t2, this.m1
     */
    final Super s3 = new Super() { 
      private int g = 10;
      { m1 = 10; } // No way to hold M1
      { t2 += 1; }
    };      
    
    @RequiresLock("TestFieldInit.this:T1, TestFieldInit.this:T2")
    public Middle1() {
      /* do nothing, but initializer for s3 is analyzed.
       * writes TestFieldInit.this.t1, TestFieldInit.this.t2, this.m1,
       * but this.m1 is masked.
       */
    }

  
  
    @RegionLock("M2 is lockM2 protects m2")
    public class Middle2 {
      public final Lock lockM2 = new ReentrantLock();
      public int m2;

      /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
       * The immediately enclosing instance with respect to Super is TestFieldInit.this
       * 
       * Writes TestFieldInit.this.t1, TestFieldInit.this.t1, Middle1.this.m1, this.m2
       */
      final Super s5 = new Super() { 
        private int g = 10;
        { m1 = 10; }
        { m2 = 20; } // No way to hold M2
        { t2 += 1; }
      };        
      
      @RequiresLock("TestFieldInit.this:T1, TestFieldInit.this:T2, test.AnonymousClass.SuperIsMember.Unqualified.OneLevel.TestFieldInit.Middle1.this:M1")
      public Middle2() {
        // do nothing, but initializer for s5 is analyzed
      }
    }
  }
}
