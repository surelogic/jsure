package test.AnonymousClass.SuperIsLocal.OneLevel;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;

/* Want to have different levels of nested of Super
 * Want to have different levels of nested of anonymous class
 */

@RegionLocks({
  @RegionLock("T1 is lockT1 protects t1"),
  @RegionLock("T2 is lockT2 protects t2")
})
public class Outer {
  public final Object lockT1 = new Object();
  public final Object lockT2 = new Object();
  public int t1;
  public int t2;
  
  @RequiresLock("T1, T2")
  public void outerMethod() {
    @RegionLock("F is lockF protects f")
    class Super {
      public final Object lockF = new Object();
      public int f;
      
      @Borrowed("this")
      @RequiresLock("Outer.this:T1")
      public Super() {
        // do stuff
        Outer.this.t1 = 5;
        this.f = 10;
      }
      
      @RequiresLock("Outer.this:T1, Outer.this:T2")
      public Super doStuff() {
        /* The immediately enclosing instance is "this" (a Super object)
         * 
         * "Outer" is the innermost lexically enclosing class of "Super"
         * "Outer" is the 1st lexically enclosing class of the class in which the
         * instance creation expression appears.
         * 
         * The immediately enclosing instance with respect to Super is Outer.this
         * 
         * Writes Outer.this.t1, Outer.this.t2
         */
        return new Super() {
          private int g = 10;
          { t2 += 1; }
        };
      }
    }
    
    /* The immediately enclosing instance of s1 is "this" (an Outer object)
     * 
     * "Outer" is the innermost lexically enclosing class of "Super"
     * "Outer" is the 0th lexically enclosing class of the class in which the
     * instance creation expression appears.
     * 
     * The immediately enclosing instance with respect to Super is Outer.this == this.
     * 
     * Writes this.t1, this.t2
     */
    final Super s1 = new Super() { 
      private int g = 10;
      { t2 += 1; }
    };
    
    
    
    @RegionLock("M1 is lockM1 protects m1")
    class Middle1 {
      public final Object lockM1 = new Object();
      public int m1;
      
      @RequiresLock("this:M1, Outer.this:T1, Outer.this:T2")
      public Super doStuff() {
        /* The immediately enclosing instance is "this" (a Middle1 object)
         * 
         * "Outer" is the innermost lexically enclosing class of "Super"
         * "Outer" is the 1st lexically enclosing class of the class in which the
         * instance creation expression appears.
         * 
         * The immediately enclosing instance with respect to Super is Outer.this
         * 
         * Writes this.m1, Outer.this:t1, Outer.this:t2
         */
        return new Super() {
          private int g = 10;
          { m1 += 1; }
          { t2 += 1; }
        };
      }
      
      
      
      @RegionLock("M2 is lockM2 protects m2")
      class Middle2 {
        public final Object lockM2 = new Object();
        public int m2;
        
        @RequiresLock("this:M2, Middle1.this:M1, Outer.this:T1, Outer.this:T2")
        public Super doStuff() {
          /* The immediately enclosing instance is "this" (a Middle2 object)
           * 
           * "Outer" is the innermost lexically enclosing class of "Super"
           * "Outer" is the 2nd lexically enclosing class of the class in which the
           * instance creation expression appears.
           * 
           * The immediately enclosing instance with respect to Super is Outer.this
           * 
           * Writes this.m2, Outer.this.t1, Outer.this.t2, Middle1.this.m1
           */
          return new Super() {
            private int g = 10;
            { m1 += 1; }
            { m2 += 1; }
            { t2 += 1; }
          };
        }
      }
    }
  }
}
