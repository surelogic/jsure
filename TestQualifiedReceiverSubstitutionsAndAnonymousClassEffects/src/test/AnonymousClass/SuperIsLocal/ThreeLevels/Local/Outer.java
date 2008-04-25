package test.AnonymousClass.SuperIsLocal.ThreeLevels.Local;

import com.surelogic.RegionEffects;

/* Want to have different levels of nested of Super
 * Want to have different levels of nested of anonymous class
 */

public class Outer {
  public int t1;
  public int t2;
  
  public void outerMostMethod() {
    class OuterLocal1 {
      public int s1;
      public int s2;
      
      public void middleMethod() {
        class OuterLocal2 {
          public int r1;
          public int r2;
          
          @RegionEffects("writes r1, r2, OuterLocal1.this:s1, OuterLocal1.this:s2, Outer.this:t1, Outer.this:t2")
          public void outerMethod() {
            class Super {
              public int f;
              
              @RegionEffects("writes Outer.this:t1, OuterLocal1.this:s1, OuterLocal2.this:r1")
              public Super() {
                // do stuff
                Outer.this.t1 = 5;
                OuterLocal1.this.s1 = 10;
                OuterLocal2.this.r1 = 15;
                this.f = 10;
              }
              
              @RegionEffects("writes OuterLocal2.this:r1, OuterLocal2.this:r2, OuterLocal1.this:s1, OuterLocal1.this:s2, Outer.this:t1, Outer.this:t2")
              public Super doStuff() {
                /* The immediately enclosing instance is "this" (a Super object)
                 * 
                 * "OuterLocal2" is the innermost lexically enclosing class of "Super"
                 * "OuterLocal2" is the 1st lexically enclosing class of the class in which the
                 * instance creation expression appears.
                 * 
                 * The immediately enclosing instance with respect to Super is OuterLocal2.this
                 * 
                 * Writes Outer.this.t1, Outer.this.t2, OuterLocal2.this.r1, OuterLocal2.this.r2, OuterLocal1.this.s1, OuterLocal1.this.s2
                 */
                return new Super() {
                  private int g = 10;
                  { t2 += 1; }
                  { s2 += 1; }
                  { r2 += 1; }
                };
              }
            }
            
            /* The immediately enclosing instance of s1 is "this" (an OuterLocal2 object)
             * 
             * "OuterLocal2" is the innermost lexically enclosing class of "Super"
             * "OuterLocal2" is the 0th lexically enclosing class of the class in which the
             * instance creation expression appears.
             * 
             * The immediately enclosing instance with respect to Super is OuterLocal2.this == this.
             * 
             * Writes this.r1, this.r2, OuterLocal1.this.s1, OuterLocal1.this.s2, Outer.this.t1, Outer.this.t2
             */
            final Super s1 = new Super() { 
              private int g = 10;
              { t2 += 1; }
              { s2 += 1; }
              { r2 += 1; }
            };
          
            class Middle1 {
              public int m1;
              
              @RegionEffects("writes this:m1, OuterLocal2.this:r1, OuterLocal2.this:r2, OuterLocal1.this:s1, OuterLocal1.this:s2, Outer.this:t1, Outer.this:t2")
              public Super doStuff() {
                /* The immediately enclosing instance is "this" (a Middle1 object)
                 * 
                 * "OuterLocal2" is the innermost lexically enclosing class of "Super"
                 * "OuterLocal2" is the 1st lexically enclosing class of the class in which the
                 * instance creation expression appears.
                 * 
                 * The immediately enclosing instance with respect to Super is OuterLocal2.this
                 * 
                 * Writes this.m1, OuterLocal2.this.r1, OuterLocal2.this.r2, any(OuterLocal1).s1, any(OuterLocal1).s2, any(Outer).t1, any(Outer).t2
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
                
                @RegionEffects("writes this:m2, Middle1.this:m1, OuterLocal2.this:r1, OuterLocal2.this:r2, OuterLocal1.this:s1, OuterLocal1.this:s2, Outer.this:t1, Outer.this:t2")
                public Super doStuff() {
                  /* The immediately enclosing instance is "this" (a Middle2 object)
                   * 
                   * "OuterLocal2" is the innermost lexically enclosing class of "Super"
                   * "OuterLocal2" is the 2nd lexically enclosing class of the class in which the
                   * instance creation expression appears.
                   * 
                   * The immediately enclosing instance with respect to Super is OuterLocal2.this
                   * 
                   * Writes this.m2, Middle1.this.m1, OuterLocal2.this.r1, OuterLocal2.this.r2, OuterLocal1.this:s1, OuterLocal1.this:s2, Outer.this:t1, Outer.this:t2
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
  }
}
