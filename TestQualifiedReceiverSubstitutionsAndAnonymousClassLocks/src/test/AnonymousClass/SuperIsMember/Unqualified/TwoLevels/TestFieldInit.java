package test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;

@RegionLocks({
  @RegionLock("T1 is lockT1 protects t1"),
  @RegionLock("T2 is lockT2 protects t2")
})
public class TestFieldInit {
  public final Object lockT1 = new Object();
  public final Object lockT2 = new Object();
  public int t1;
  public int t2;
  

  @RegionLocks({
    @RegionLock("C1 is lockC1 protects c1"),
    @RegionLock("C2 is lockC2 protects c2")
  })
  public class Container {
    public final Object lockC1 = new Object();
    public final Object lockC2 = new Object();
    public int c1;
    public int c2;
    
    @RegionLock("F is lockF protects f")
    public class Super {
      public final Object lockF = new Object();
      public int f;
      
      { 
        Container.this.c1 = 10;
      }
      
      @Borrowed("this")
      @RequiresLock("TestFieldInit.this:T1, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.TestFieldInit.Container.this:C1")
      public Super() {
        // do stuff
        TestFieldInit.this.t1 = 5;
        this.f = 10;
      }
    }
  
    
    
    /* The immediately enclosing instance of s1 is "this" (a Container object)
     * The immediately enclosing instance with respect to Super is Container.this.
     * 
     * Writes TestFieldInit.this.t1, TestFieldInit.this.t2, this.c1, this.c2
     */
    final Super s1 = new Super() { // No way to hold C1
      private int g = 10;
      { t2 += 1; }
      { c2 = 9; } // No way to hold C2
    };

    @RequiresLock("TestFieldInit.this:T1, TestFieldInit.this:T2")
    public Container() {
      // Effects from field init
    }
  
    
    
    @RegionLock("M1 is lockM1 protects m1")
    public class Middle1 {
      public final Object lockM1 = new Object();
      public int m1;
      
      /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
       * The immediately enclosing instance with respect to Super is Container.this.
       * 
       * Writes TestFieldInit.this.t1, TestFieldInit.this.t2, Container.this.c1, Container.this.c2, this.m1
       */
      final Super s3 = new Super() { 
        private int g = 10;
        { m1 = 10; } // No way to hold M1
        { t2 += 1; }
        { c2 = 9; }
      };

      @RequiresLock("TestFieldInit.this:T1, TestFieldInit.this:T2, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.TestFieldInit.Container.this:C1, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.TestFieldInit.Container.this:C2")
      public Middle1() {
        // Effects from field init
      }
  
    
    
      @RegionLock("M2 is lockM2 protects m2")
      public class Middle2 {
        public final Object lockM2 = new Object();
        public int m2;
        
        /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
         * The immediately enclosing instance with respect to Super is Container.this.
         * 
         * Writes TestFieldInit.this.t1, TestFieldInit.this.t2, Middle1.this.m1, Container.this.c1, Container.this.c2, this.m2
         */
        final Super s5 = new Super() { 
          private int g = 10;
          { m1 = 10; }
          { m2 = 20; } // No way to hold M2
          { t2 += 1; }
          { c2 = 9; }
        };

        @RequiresLock("TestFieldInit.this:T1, TestFieldInit.this:T2, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.TestFieldInit.Container.Middle1.this:M1, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.TestFieldInit.Container.this:C1, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.TestFieldInit.Container.this:C2")
        public Middle2() {
          // effects from field init
        }
      }
    }
  }
}
