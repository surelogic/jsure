package test.AnonymousClass.SuperIsLocal.OneLevel;

import com.surelogic.RegionEffects;

/* Want to have different levels of nested of Super
 * Want to have different levels of nested of anonymous class
 */

public class Outer {
  public int t1;
  public int t2;
  
  @RegionEffects("writes this:t1, this:t2")
  public void outerMethod() {
    class Super {
      public int f;
      
      @RegionEffects("writes Outer.this:t1")
      public Super() {
        // do stuff
        Outer.this.t1 = 5;
        this.f = 10;
      }
      
      @RegionEffects("writes Outer.this:t1, Outer.this:t2")
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
    
    class Middle1 {
      public int m1;
      
      @RegionEffects("writes this:m1, Outer.this:t1, Outer.this:t2")
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
      
      class Middle2 {
        public int m2;
        
        @RegionEffects("writes this:m2, any(Middle1):m1, Outer.this:t1, Outer.this:t2")
        public Super doStuff() {
          /* The immediately enclosing instance is "this" (a Middle2 object)
           * 
           * "Outer" is the innermost lexically enclosing class of "Super"
           * "Outer" is the 2nd lexically enclosing class of the class in which the
           * instance creation expression appears.
           * 
           * The immediately enclosing instance with respect to Super is Outer.this
           * 
           * Writes this.m2, Outer.this.t1, Outer.this.t2, any(Middle1).m1
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
