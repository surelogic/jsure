package test.AnonymousClass.SuperIsLocal.TwoLevels.DirectInner;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;
import com.surelogic.SingleThreaded;

/* Want to have different levels of nested of Super -- in direct inner classes and in local classes!
 * Want to have different levels of nested of anonymous class
 */

@RegionLocks({
  @RegionLock("T1 is lockT1 protects t1"),
  @RegionLock("T2 is lockT2 protects t2")
})
public class Outer1 {
  public final Object lockT1 = new Object();
  public final Object lockT2 = new Object();
  public int t1;
  public int t2;
  
  @RegionLocks({
    @RegionLock("S1 is lockS1 protects s1"),
    @RegionLock("S2 is lockS2 protects s2")
  })
  public class Outer2 {
    public final Object lockS1 = new Object();
    public final Object lockS2 = new Object();
    public int s1;
    public int s2;
    
    @RequiresLock("Outer1.this:T1, Outer1.this:T2, this:S1, this:S2")
    public void outerMethod() {
      @RegionLock("F is lockF protects f")
      class Super {
        public final Object lockF = new Object();
        public int f;
        
        @SingleThreaded
        @Borrowed("this")
        @RequiresLock("Outer1.this:T1, Outer2.this:S1")
        public Super() {
          // do stuff
          Outer1.this.t1 = 5;
          Outer2.this.s1 = 10;
          this.f = 10;
        }
        
        @RequiresLock("Outer1.this:T1, Outer1.this:T2, Outer2.this:S1, Outer2.this:S2")
        public Super doStuff() {
          /* The immediately enclosing instance is "this" (a Super object)
           * 
           * "Outer2" is the innermost lexically enclosing class of "Super"
           * "Outer2" is the 1st lexically enclosing class of the class in which the
           * instance creation expression appears.
           * 
           * The immediately enclosing instance with respect to Super is Outer2.this
           * 
           * Writes Outer1.this.t1, Outer1.this.t2, Outer2.this.s1, Outer2.this.s2
           */
          return new Super() {
            private int g = 10;
            { t2 += 1; }
            { s2 += 1; }
          };
        }
      }
      
      /* The immediately enclosing instance of s1 is "this" (an Outer2 object)
       * 
       * "Outer2" is the innermost lexically enclosing class of "Super"
       * "Outer2" is the 0th lexically enclosing class of the class in which the
       * instance creation expression appears.
       * 
       * The immediately enclosing instance with respect to Super is Outer2.this == this.
       * 
       * Writes this.s1, this.s2, any(Outer1).s1, any(Outer1).s2
       */
      final Super s1 = new Super() { 
        private int g = 10;
        { t2 += 1; }
        { s2 += 1; }
      };
      
      
      
      @RegionLock("M1 is lockM1 protects m1")
      class Middle1 {
        public final Object lockM1 = new Object();
        public int m1;
        
        @RequiresLock("this:M1, Outer2.this:S1, Outer2.this:S2, Outer1.this:T1, Outer1.this:T2")
        public Super doStuff() {
          /* The immediately enclosing instance is "this" (a Middle1 object)
           * 
           * "Outer2" is the innermost lexically enclosing class of "Super"
           * "Outer2" is the 1st lexically enclosing class of the class in which the
           * instance creation expression appears.
           * 
           * The immediately enclosing instance with respect to Super is Outer2.this
           * 
           * Writes this.m1, Outer2.this.s1, Outer2.this.s2, Outer1.this.t1, Outer1.this.t2
           */
          return new Super() {
            private int g = 10;
            { m1 += 1; }
            { t2 += 1; }
            { s2 += 1; }
          };
        }
        
        
        
        @RegionLock("M2 is lockM2 protects m2")
        class Middle2 {
          public final Object lockM2 = new Object();
          public int m2;
          
          @RequiresLock("this:M2, Middle1.this:M1, Outer2.this:S1, Outer2.this:S2, Outer1.this:T1, Outer1.this:T2")
          public Super doStuff() {
            /* The immediately enclosing instance is "this" (a Middle2 object)
             * 
             * "Outer2" is the innermost lexically enclosing class of "Super"
             * "Outer2" is the 2nd lexically enclosing class of the class in which the
             * instance creation expression appears.
             * 
             * The immediately enclosing instance with respect to Super is Outer2.this
             * 
             * Writes Middle1.this.m1, this.m2, Outer1.this.t1, Outer1.this.t2, Outer2.this.s1, Outer2.this.s2
             */
            return new Super() {
              private int g = 10;
              { m1 += 1; }
              { m2 += 1; }
              { t2 += 1; }
              { s2 += 1; }
            };
          }
        }
      }
    }
  }
}
