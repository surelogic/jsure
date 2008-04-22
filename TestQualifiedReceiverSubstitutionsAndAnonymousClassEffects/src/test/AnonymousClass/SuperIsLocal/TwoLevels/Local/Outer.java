package test.AnonymousClass.SuperIsLocal.TwoLevels.Local;

import com.surelogic.RegionEffects;

/* Want to have different levels of nested of Super
 * Want to have different levels of nested of anonymous class
 */

public class Outer {
  public int t1;
  public int t2;
  
//  public static void main(String[] args) {
//    final Outer outer = new Outer();
//    System.out.println("outer = " + outer);
//    outer.outerMostMethod();
//  }
  
  public void outerMostMethod() {
    class OuterLocal {
      public int s1;
      public int s2;
      
      @RegionEffects("writes s1, s2, Outer.this:t1, Outer.this:t2")
      public void outerMethod() {
        class Super {
          public int f;
          
          @RegionEffects("writes Outer.this:t1, OuterLocal.this:s1")
          public Super() {
//            System.out.println("** Outer.this = " + Outer.this);
//            System.out.println("** OuterLocal.this = " + OuterLocal.this);
            // do stuff
            Outer.this.t1 = 5;
            OuterLocal.this.s1 = 10;
            this.f = 10;
          }
          
          @RegionEffects("writes OuterLocal.this:s1, OuterLocal.this:s2, Outer.this:t1, Outer.this:t2")
          public Super doStuff() {
            /* The immediately enclosing instance is "this" (a Super object)
             * 
             * "OuterLocal" is the innermost lexically enclosing class of "Super"
             * "OuterLocal" is the 1st lexically enclosing class of the class in which the
             * instance creation expression appears.
             * 
             * The immediately enclosing instance with respect to Super is OuterLocal.this
             * 
             * Writes Outer.this.t1, Outer.this.t2, OuterLocal.this.s1, OuterLocal.this.s2
             */
            return new Super() {
              private int g = 10;
              { t2 += 1; }
              { s2 += 1; }
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
         * Writes this.s1, this.s2, any(Outer).
         */
        final Super s1 = new Super() { 
          private int g = 10;
          { t2 += 1; }
          { s2 += 1; }
        };
      
        class Middle1 {
          public int m1;
          
          @RegionEffects("writes this:m1, OuterLocal.this:s1, OuterLocal.this:s2, Outer.this:t1, Outer.this:t2")
          public Super doStuff() {
            /* The immediately enclosing instance is "this" (a Middle1 object)
             * 
             * "OuterLocal" is the innermost lexically enclosing class of "Super"
             * "OuterLocal" is the 1st lexically enclosing class of the class in which the
             * instance creation expression appears.
             * 
             * The immediately enclosing instance with respect to Super is OuterLocal.this
             * 
             * Writes this.m1, OuterLocal.this.s1, OuterLocal.this.s2, Outer.this.t1, Outer.this.t2
             */
            return new Super() {
              private int g = 10;
              { m1 += 1; }
              { t2 += 1; }
              { s2 += 1; }
//              {
//                System.out.println("* Outer.this = " + Outer.this);
//                System.out.println("* OuterLocal.this = " + OuterLocal.this);
//              }
            };
          }
                    
          class Middle2 {
            public int m2;
            
            @RegionEffects("writes this:m2, Middle1.this:m1, OuterLocal.this:s1, OuterLocal.this:s2, Outer.this:t1, Outer.this:t2")
            public Super doStuff() {
              /* The immediately enclosing instance is "this" (a Middle2 object)
               * 
               * "OuterLocal" is the innermost lexically enclosing class of "Super"
               * "OuterLocal" is the 2nd lexically enclosing class of the class in which the
               * instance creation expression appears.
               * 
               * The immediately enclosing instance with respect to Super is OuterLocal.this
               * 
               * Writes this.m2, Middle1.this.m1, OuterLocal.this.s1, OuterLocal.this.s2, Outer.this.t1, Outer.this.t2
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
//        
//        final Middle1 m1 = new Middle1();
//        m1.doStuff();
      }
    }
//    
//    final OuterLocal outerLocal = new OuterLocal();
//    System.out.println("outerLocal = " + outerLocal);
//    outerLocal.outerMethod();
  }
}
