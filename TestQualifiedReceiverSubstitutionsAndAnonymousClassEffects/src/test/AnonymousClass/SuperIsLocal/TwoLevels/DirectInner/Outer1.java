package test.AnonymousClass.SuperIsLocal.TwoLevels.DirectInner;

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
    
    @RegionEffects("writes any(Outer1):t1, any(Outer1):t2, this:s1, this:s2")
    public void outerMethod() {
      class Super {
        public int f;
        
        @RegionEffects("writes Outer1.this:t1, Outer2.this:s1")
        public Super() {
          // do stuff
          Outer1.this.t1 = 5;
          Outer2.this.s1 = 10;
          this.f = 10;
        }
        
        @RegionEffects("writes any(Outer1):t1, any(Outer1):t2, Outer2.this:s1, Outer2.this:s2")
        public Super doStuff() {
          /* The immediately enclosing instance is "this" (a Super object)
           * 
           * "Outer2" is the innermost lexically enclosing class of "Super"
           * "Outer2" is the 1st lexically enclosing class of the class in which the
           * instance creation expression appears.
           * 
           * The immediately enclosing instance with respect to Super is Outer2.this
           * 
           * Writes any(Outer1).t1, any(Outer1).t2, Outer2.this.s1, Outer2.this.s2
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
      
      class Middle1 {
        public int m1;
        
        @RegionEffects("writes this:m1, Outer2.this:s1, Outer2.this:s2, any(Outer1):t1, any(Outer1):t2")
        public Super doStuff() {
          /* The immediately enclosing instance is "this" (a Middle1 object)
           * 
           * "Outer2" is the innermost lexically enclosing class of "Super"
           * "Outer2" is the 1st lexically enclosing class of the class in which the
           * instance creation expression appears.
           * 
           * The immediately enclosing instance with respect to Super is Outer2.this
           * 
           * Writes this.m1, Outer2.this.s1, Outer2.this.s2, any(Outer1).t1, any(Outer1).t2
           */
          return new Super() {
            private int g = 10;
            { m1 += 1; }
            { t2 += 1; }
            { s2 += 1; }
          };
        }
        
        class Middle2 {
          public int m2;
          
          @RegionEffects("writes this:m2, any(Middle1):m1, Outer2.this:s1, Outer2.this:s2, any(Outer1):t1, any(Outer1):t2")
          public Super doStuff() {
            /* The immediately enclosing instance is "this" (a Middle2 object)
             * 
             * "Outer2" is the innermost lexically enclosing class of "Super"
             * "Outer2" is the 2nd lexically enclosing class of the class in which the
             * instance creation expression appears.
             * 
             * The immediately enclosing instance with respect to Super is Outer2.this
             * 
             * Writes any(Middle1).m1, this.m2, any(Outer1).t1, any(Outer1).t2, Outer2.this.s1, Outer2.this.s2
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
