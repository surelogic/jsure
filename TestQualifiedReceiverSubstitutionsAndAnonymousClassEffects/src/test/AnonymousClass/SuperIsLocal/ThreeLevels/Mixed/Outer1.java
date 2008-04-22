package test.AnonymousClass.SuperIsLocal.ThreeLevels.Mixed;

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
    
    public void outerMostMethod() {
      class OuterLocal {
        public int r1;
        public int r2;
        
        
        @RegionEffects("writes r1, r2, Outer1.this:t1, Outer1.this:t2, Outer2.this:s1, Outer2.this:s2")
        public void outerMethod() {
          class Super {
            public int f;
            
            @RegionEffects("writes Outer1.this:t1, Outer2.this:s1, OuterLocal.this:r1")
            public Super() {
              // do stuff
              Outer1.this.t1 = 5;
              Outer2.this.s1 = 10;
              OuterLocal.this.r1 = 15;
              this.f = 10;
            }
            
            @RegionEffects("writes Outer1.this:t1, Outer1.this:t2, Outer2.this:s1, Outer2.this:s2, OuterLocal.this:r1, OuterLocal.this:r2")
            public Super doStuff() {
              /* The immediately enclosing instance is "this" (a Super object)
               * 
               * "OuterLocal" is the innermost lexically enclosing class of "Super"
               * "OuterLocal" is the 1st lexically enclosing class of the class in which the
               * instance creation expression appears.
               * 
               * The immediately enclosing instance with respect to Super is OuterLocal.this
               * 
               * Writes Outer1.this.t1, Outer1.this.t2, Outer2.this.s1, Outer2.this.s2, OuterLocal.this.r1, OuterLocal.this.r2
               */
              return new Super() {
                private int g = 10;
                { t2 += 1; }
                { s2 += 1; }
                { r2 += 1; }
              };
            }
          }
          
          /* The immediately enclosing instance of s1 is "this" (an OuterLocal object)
           * 
           * "OuterLocal" is the innermost lexically enclosing class of "Super"
           * "OuterLocal" is the 0th lexically enclosing class of the class in which the
           * instance creation expression appears.
           * 
           * The immediately enclosing instance with respect to Super is OuterLocal.this == this.
           * 
           * Writes this.r1, this.r2, Outer1.this:t1, Outer1.this:t2, Outer2.this:s1, Outer2.this:s2
           */
          final Super s1 = new Super() { 
            private int g = 10;
            { t2 += 1; }
            { s2 += 1; }
            { r2 += 1; }
          };
          
          class Middle1 {
            public int m1;
            
            @RegionEffects("writes this:m1, OuterLocal.this:r1, OuterLocal.this:r2, Outer1.this:t1, Outer1.this:t2, Outer2.this:s1, Outer2.this:s2")
            public Super doStuff() {
              /* The immediately enclosing instance is "this" (a Middle1 object)
               * 
               * "OuterLocal" is the innermost lexically enclosing class of "Super"
               * "OuterLocal" is the 1st lexically enclosing class of the class in which the
               * instance creation expression appears.
               * 
               * The immediately enclosing instance with respect to Super is OuterLocal.this
               * 
               * Writes this.m1, OuterLocal.this.r1, Outer1.this:t1, Outer1.this:t2, Outer2.this:s1, Outer2.this:s2
               */
              return new Super() {
                private int g = 10;
                { m1 += 1; }
                { t2 += 1; }
                { s2 += 1; }
                { r1 += 1; }
              };
            }
            
            class Middle2 {
              public int m2;
              
              @RegionEffects("writes this:m2, Middle1.this:m1, OuterLocal.this:r1, OuterLocal.this:r2, Outer1.this:t1, Outer1.this:t2, Outer2.this:s1, Outer2.this:s2")
              public Super doStuff() {
                /* The immediately enclosing instance is "this" (a Middle2 object)
                 * 
                 * "OuterLocal" is the innermost lexically enclosing class of "Super"
                 * "OuterLocal" is the 2nd lexically enclosing class of the class in which the
                 * instance creation expression appears.
                 * 
                 * The immediately enclosing instance with respect to Super is OuterLocal.this
                 * 
                 * Writes Middle1.this.m1, this.m2, OuterLocal.this.r1, Outer1.this:t1, Outer1.this:t2, Outer2.this:s1, Outer2.this:s2
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
  }
}
