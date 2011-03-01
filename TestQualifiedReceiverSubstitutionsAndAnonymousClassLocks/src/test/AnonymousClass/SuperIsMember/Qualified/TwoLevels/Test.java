package test.AnonymousClass.SuperIsMember.Qualified.TwoLevels;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;

@RegionLocks({
  @RegionLock("T1 is lockT1 protects t1"),
  @RegionLock("T2 is lockT2 protects t2")
})
public class Test {
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
      
      // Writes Test.this.t1, Container.this.c1
      @Borrowed("this")
      @RequiresLock("test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test.this:T1, test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test.Container.this:C1")
      public Super() {
        // do stuff
        Test.this.t1 = 5;
        this.f = 10;
      }
    }
  
    public Container() {}
    
    @RequiresLock("other:C1, other:C2")
    public Container(final Container other) {
      /* The immediately enclosing instance of s1 is "this" (a Container object)
       * The immediately enclosing instance with respect to Super is other.
       * 
       * Writes any(Test).t1, any(Test).t2, other.c1, other.c2
       */
      final Super s1 = other. new Super() { // T1 not resolvable
        private int g = 10;
        { t2 += 1; }  // T2 not resolvable
        { c2 = 9; }
      };
    }
  
    @RequiresLock("other:C1, other:C2")
    public void stuff(final Container other) {
      /* The immediately enclosing instance of s2 is "this" (a Container object)
       * The immediately enclosing instance with respect to Super is other.
       * 
       * Writes any(Test).t1, any(Test).t2, other.c1, other.c2
       */
      final Super s2 = other. new Super() {  // T1 not resolvable
        private int g = 10;
        { t2 += 1; }  // T2 not resolvable
        { c2 = 9; }
      };
    }
  
    
    
    @RegionLock("M1 is lockM1 protects m1")
    public class Middle1 {
      public final Object lockM1 = new Object();
      public int m1;
      
      @RequiresLock("other:C1, other:C2")
      public Middle1(final Container other) {
        /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
         * The immediately enclosing instance with respect to Super is other.
         * 
         * Writes any(Test).t1, ant(Test).t2, other.c1, other.c2, this.m1
         */
        synchronized (lockM1) {
          final Super s3 = other. new Super() { // T1 not resolvable
            private int g = 10;
            { m1 = 10; }
            { t2 += 1; } // T2 not resolvable
            { c2 = 9; }
          };
        }
      }
  
      @RequiresLock("other:C1, other:C2, this:M1")
      public void stuff(final Container other) {
        /* The immediately enclosing instance of s4 is "this" (a Middle1 object)
         * The immediately enclosing instance with respect to Super is other.
         * 
         * Writes any(Test).t1, ant(Test).t2, other.c1, other.c2, this.m1
         */
        final Super s4 = other. new Super() {  // T1 not resolvable
          private int g = 10;
          { m1 = 20; }
          { t2 += 1; } // T2 not resolvable
          { c2 = 9; }
        };
      }
  
    
    
      @RegionLock("M2 is lockM2 protects m2")
      public class Middle2 {
        public final Object lockM2 = new Object();
        public int m2;
        
        @RequiresLock("other:C1, other:C2")
        public Middle2(final Container other) {
          /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
           * The immediately enclosing instance with respect to Super is other.
           * 
           * Writes any(Test).t1, any(Test).t2, any(Middle1).m1, other.c1, other.c2, this.m2
           */
          synchronized (lockM2) {
            final Super s5 = other. new Super() { // T1 not resolvable
              private int g = 10;
              { m1 = 10; } // M1 not resolvable
              { m2 = 20; }
              { t2 += 1; } // T2 not resolvable
              { c2 = 9; }
            };
          }
        }
  
        @RequiresLock("other:C1, other:C2, this:M2")
        public void stuff(final Container other) {
          /* The immediately enclosing instance of s6 is "this" (a Middle2 object)
           * The immediately enclosing instance with respect to Super is other.
           * 
           * Writes any(Test).t1, any(Test).t2, any(Middle1).m1, other.c1, other.c2, this.m2
           */
          final Super s6 = other. new Super() { // T1 not resolvable
            private int g = 10;
            { m1 = 20; } // M1 not resolvable
            { m2 = 30; }
            { t2 += 1; } // M2 not resolvable
            { c2 = 9; }
          };
        }
      }
    }
  }
}
