package test.AnonymousClass.SuperIsLocal.TwoLevels.Local;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionEffects;
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
  public final Lock lockT1 = new ReentrantLock();
  public final Lock lockT2 = new ReentrantLock();
  public int t1;
  public int t2;
  
  public void outerMostMethod() {
    @RegionLocks({
      @RegionLock("S1 is lockS1 protects s1"),
      @RegionLock("S2 is lockS2 protects s2")
    })
    class OuterLocal {
      public final Lock lockS1 = new ReentrantLock();
      public final Lock lockS2 = new ReentrantLock();
      public int s1;
      public int s2;
      
      @RequiresLock("S1, S2, Outer.this:T1, Outer.this:T2")
      public void outerMethod() {
        class Super {
          public int f;
          
          @RequiresLock("Outer.this:T1, OuterLocal.this:S1")
          public Super() {
            // do stuff
            Outer.this.t1 = 5;
            OuterLocal.this.s1 = 10;
            this.f = 10;
          }
          
          @RequiresLock("OuterLocal.this:S1, OuterLocal.this:S2, Outer.this:T1, Outer.this:T2")
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
          
          public Super doStuff2() {
            lockS1.lock();
            try {
              lockS2.lock();
              try {
                lockT1.lock();
                try {
                  lockT2.lock();
                  try {
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
                  } finally {
                    lockT2.unlock();
                  }
                } finally {
                  lockT1.unlock();
                }
              } finally {
                lockS2.unlock();
              }
            } finally {
              lockS1.unlock();
            }
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
      
        
        
        @RegionLock("M1 is lockM1 protects m1")
        class Middle1 {
          public final Lock lockM1 = new ReentrantLock();
          public int m1;
          
          @RequiresLock("this:M1, OuterLocal.this:S1, OuterLocal.this:S2, Outer.this:T1, Outer.this:T2")
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
            };
          }

          public Super doStuff2() {
            lockS1.lock();
            try {
              lockS2.lock();
              try {
                lockT1.lock();
                try {
                  lockT2.lock();
                  try {
                    lockM1.lock();
                    try {
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
                      };
                    } finally {
                      lockM1.unlock();
                    }
                  } finally {
                    lockT2.unlock();
                  }
                } finally {
                  lockT1.unlock();
                }
              } finally {
                lockS2.unlock();
              }
            } finally {
              lockS1.unlock();
            }
          }          
          
          @RegionLock("M2 is lockM2 protects m2")
          class Middle2 {
            public final Lock lockM2 = new ReentrantLock();
            public int m2;
            
            @RequiresLock("this:M2, Middle1.this:M1, OuterLocal.this:S1, OuterLocal.this:S2, Outer.this:T1, Outer.this:T2")
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
            
            public Super doStuff2() {
              lockS1.lock();
              try {
                lockS2.lock();
                try {
                  lockT1.lock();
                  try {
                    lockT2.lock();
                    try {
                      lockM1.lock();
                      try {
                        lockM2.lock();
                        try {
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
                        } finally {
                          lockM2.unlock();
                        }
                      } finally {
                        lockM1.unlock();
                      }
                    } finally {
                      lockT2.unlock();
                    }
                  } finally {
                    lockT1.unlock();
                  }
                } finally {
                  lockS2.unlock();
                }
              } finally {
                lockS1.unlock();
              }
            }  
          }
        }
        
        final Middle1 m1 = new Middle1();
        m1.lockM1.lock();
        try {
          m1.doStuff(); // S1, S2, T1, T2 are not resolvable
        } finally {
          m1.lockM1.unlock();
        }
      }
    }
  }
}
