package test.AnonymousClass.SuperIsLocal.TwoLevels.DirectInner;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;

/* Want to have different levels of nested of Super -- in direct inner classes and in local classes!
 * Want to have different levels of nested of anonymous class
 */

@RegionLocks({
  @RegionLock("T1 is lockT1 protects t1"),
  @RegionLock("T2 is lockT2 protects t2")
})
public class Outer1 {
  public final Lock lockT1 = new ReentrantLock();
  public final Lock lockT2 = new ReentrantLock();
  public int t1;
  public int t2;
  
  @RegionLocks({
    @RegionLock("S1 is lockS1 protects s1"),
    @RegionLock("S2 is lockS2 protects s2")
  })
  public class Outer2 {
    public final Lock lockS1 = new ReentrantLock();
    public final Lock lockS2 = new ReentrantLock();
    public int s1;
    public int s2;
    
    @RequiresLock("Outer1.this:T1, Outer1.this:T2, this:S1, this:S2")
    public void outerMethod() {
      @RegionLock("F is lockF protects f")
      class Super {
        public final Lock lockF = new ReentrantLock();
        public int f;
        
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
        
        public Super doStuff2() {
          lockT1.lock();
          try {
            lockT2.lock();
            try {
              lockS1.lock();
              try {
                lockS2.lock();
                try {
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
                } finally {
                  lockS2.unlock();
                }
              } finally {
                lockS1.unlock();
              }
            } finally {
              lockT2.unlock();
            }
          } finally {
            lockT1.unlock();
          }
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
        public final Lock lockM1 = new ReentrantLock();
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
        
        public Super doStuff2() {
          lockT1.lock();
          try {
            lockT2.lock();
            try {
              lockS1.lock();
              try {
                lockS2.lock();
                try {
                  lockM1.lock();
                  try {
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
                  } finally {
                    lockM1.unlock();
                  }
                } finally {
                  lockS2.unlock();
                }
              } finally {
                lockS1.unlock();
              }
            } finally {
              lockT2.unlock();
            }
          } finally {
            lockT1.unlock();
          }
        }
        
        
        @RegionLock("M2 is lockM2 protects m2")
        class Middle2 {
          public final Lock lockM2 = new ReentrantLock();
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
          
          public Super doStuff2() {
            lockT1.lock();
            try {
              lockT2.lock();
              try {
                lockS1.lock();
                try {
                  lockS2.lock();
                  try {
                    lockM1.lock();
                    try {
                      lockM2.lock();
                      try {
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
                      } finally {
                        lockM2.unlock();
                      }
                    } finally {
                      lockM1.unlock();
                    }
                  } finally {
                    lockS2.unlock();
                  }
                } finally {
                  lockS1.unlock();
                }
              } finally {
                lockT2.unlock();
              }
            } finally {
              lockT1.unlock();
            }
          }
        }
      }
    }
  }
}
