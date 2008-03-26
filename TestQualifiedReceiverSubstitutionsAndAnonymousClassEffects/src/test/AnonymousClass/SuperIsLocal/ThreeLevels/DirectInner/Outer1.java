package test.AnonymousClass.SuperIsLocal.ThreeLevels.DirectInner;

import com.surelogic.RegionEffects;

/* Want to have different levels of nested of Super -- in direct inner classes and in local classes!
 * Want to have different levels of nested of anonymous class
 */

public class Outer1 {
  public int t1;
  public int t2;
  
  public class Outer2 {
    public int s1;
    public int s2;
    
    public class Outer3 {
      public int r1;
      public int r2;
      
      @RegionEffects("writes any(Outer1):t1, any(Outer1):t2, any(Outer2):s1, any(Outer2):s2, this:r1, this:r2")
      public void outerMethod() {
        class Super {
          public int f;
          
          @RegionEffects("writes Outer1.this:t1, Outer2.this:s1, Outer3.this:r1")
          public Super() {
            // do stuff
            Outer1.this.t1 = 5;
            Outer2.this.s1 = 10;
            Outer3.this.r1 = 15;
            this.f = 10;
          }
          
          @RegionEffects("writes any(Outer1):t1, any(Outer1):t2, any(Outer2):s1, any(Outer2):s2, Outer3.this:r1, Outer3.this:r2")
          public Super doStuff() {
            /* The immediately enclosing instance is "this" (a Super object)
             * 
             * "Outer3" is the innermost lexically enclosing class of "Super"
             * "Outer3" is the 1st lexically enclosing class of the class in which the
             * instance creation expression appears.
             * 
             * The immediately enclosing instance with respect to Super is Outer3.this
             * 
             * Writes any(Outer1).t1, any(Outer1).t2, any(Outer2).s1, any(Outer2).s2,
             *        Outer3.this.r1, Outer3.this.r2
             */
            return new Super() {
              private int g = 10;
              { t2 += 1; }
              { s2 += 1; }
              { r2 += 1; }
            };
          }
        }
        
        /* The immediately enclosing instance of s1 is "this" (an Outer3 object)
         * 
         * "Outer3" is the innermost lexically enclosing class of "Super"
         * "Outer3" is the 0th lexically enclosing class of the class in which the
         * instance creation expression appears.
         * 
         * The immediately enclosing instance with respect to Super is Outer3.this == this.
         * 
         * Writes this.r1, this.r2, any(Outer2).s1, any(Outer2).s2, any(Outer1).t1, any(Outer1).t2
         */
        final Super s1 = new Super() { 
          private int g = 10;
          { t2 += 1; }
          { s2 += 1; }
          { r2 += 1; }
        };
        
        class Middle1 {
          public int m1;
          
          @RegionEffects("writes this:m1, any(Outer1):t1, any(Outer1):t2, any(Outer2):s1, any(Outer2):s2, Outer3.this:r1, Outer3.this:r2")
          public Super doStuff() {
            /* The immediately enclosing instance is "this" (a Middle1 object)
             * 
             * "Outer3" is the innermost lexically enclosing class of "Super"
             * "Outer3" is the 1st lexically enclosing class of the class in which the
             * instance creation expression appears.
             * 
             * The immediately enclosing instance with respect to Super is Outer3.this
             * 
             * Writes this.m1, Outer3.this.r1, Outer3.this.r2, any(Outer2).s1, any(Outer2).s2, any(Outer1).t1, any(Outer1).t2
             */
            return new Super() {
              private int g = 10;
              { m1 += 1; }
              { t2 += 1; }
              { s2 += 1; }
              { r2 += 1; }
            };
          }
          
          class Middle2 {
            public int m2;
            
            @RegionEffects("writes this:m2, any(Middle1):m1, any(Outer1):t1, any(Outer1):t2, any(Outer2):s1, any(Outer2):s2, Outer3.this:r1, Outer3.this:r2")
            public Super doStuff() {
              /* The immediately enclosing instance is "this" (a Middle2 object)
               * 
               * "Outer3" is the innermost lexically enclosing class of "Super"
               * "Outer3" is the 2nd lexically enclosing class of the class in which the
               * instance creation expression appears.
               * 
               * The immediately enclosing instance with respect to Super is Outer3.this
               * 
               * Writes any(Middle1).m1, this.m2, Outer3.this.r1, Outer3.this.r2, any(Outer2).s1, any(Outer2).s2, any(Outer1).t1, any(Outer1).t2
               */
              return new Super() {
                private int g = 10;
                { m1 += 1; }
                { m2 += 1; }
                { t2 += 1; }
                { s2 += 1; }
                { r2 += 1; }
              };
            }
          }
        }
      }
    }
  }
}
