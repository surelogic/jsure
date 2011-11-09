package test.AnonymousClass.SuperIsNotInner;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;

@RegionLocks({
  @RegionLock("SF1 is lockSF1 protects staticField1"),
  @RegionLock("SF2 is lockSF2 protects staticField2"),
  @RegionLock("SF3 is lockSF3 protects staticField3")
})
public class StaticContext {
  public static final Lock lockSF1 = new ReentrantLock();
  public static final Lock lockSF2 = new ReentrantLock();
  public static final Lock lockSF3 = new ReentrantLock();
  public static int staticField1 = 100;
  public static int staticField2 = 200;
  public static int staticField3 = 300;
  
  public int nonStaticField = 1;
  
  
  
  
  /* s1 has no immediately enclosing instance.  
   * s1 has no immediately enclosing instance with respect to Super.
   * 
   * Writes Super.count, StaticContext.staticField2; reads StaticContext.staticField1
   */
  /* Needs SF1, SF2, and COUNT.  Holds SF1 and SF2 (and SF3) by virtue of being
   * part of the class initialization.  DOES NOT HOLD COUNT.
   */
  public static Super s1 = new Super() {
    private int f = staticField1;
    private int g;
    
    { 
      g = staticField2++;
    }
    
    @RequiresLock("test.AnonymousClass.SuperIsNotInner.StaticContext:SF3")
    public int foo1() {
      return staticField3;
    }

    public int foo2() {
      lockSF3.lock();
      try {
        return staticField3;
      } finally {
        lockSF3.unlock();
      }
    }
  };
  
  
  
  
  static {
    /* s2 has no immediately enclosing instance.  
     * s2 has no immediately enclosing instance with respect to Super.
     */
    /* Needs SF1, SF2, and COUNT.  Holds SF1 and SF2 (and SF3) by virtue of being
     * part of the class initialization.  Need to explicitly acquire COUNT.
     */
    Super.lockCount.lock();
    try {
      final Super s2 = new Super() {
        private int f = staticField1;
        private int g;
        
        { 
          g = staticField2++;
        }
        
        @RequiresLock("test.AnonymousClass.SuperIsNotInner.StaticContext:SF3")
        public int foo3() {
          return staticField3;
        }

        public int foo4() {
          lockSF3.lock();
          try {
            return staticField3;
          } finally {
            lockSF3.unlock();
          }
        }
      };
    } finally {
      Super.lockCount.unlock();
    }
  }
  
  
  
  
  @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT, test.AnonymousClass.SuperIsNotInner.StaticContext:SF1, test.AnonymousClass.SuperIsNotInner.StaticContext:SF2")
  public static void doStuff1() {
    /* s3 has no immediately enclosing instance.  
     * s3 has no immediately enclosing instance with respect to Super.
     * 
     * Writes Super.count, StaticContext.staticField2; reads StaticContext.staticField1
     */
    final Super s3 = new Super() {
      private int f = staticField1;
      private int g;
      
      { 
        g = staticField2++;
      }
      
      @RequiresLock("test.AnonymousClass.SuperIsNotInner.StaticContext:SF3")
      public int foo5() {
        return staticField3;
      }

      public int foo6() {
        lockSF3.lock();
        try {
          return staticField3;
        } finally {
          lockSF3.unlock();
        }
      }
    };
  }

  public static void doStuff2() {
    Super.lockCount.lock();
    try {
      lockSF1.lock();
      try {
        lockSF2.lock();
        try {
          lockSF3.lock();
          try {
            /* s3 has no immediately enclosing instance.  
             * s3 has no immediately enclosing instance with respect to Super.
             * 
             * Writes Super.count, StaticContext.staticField2; reads StaticContext.staticField1
             */
            final Super s3 = new Super() {
              private int f = staticField1;
              private int g;
              
              { 
                g = staticField2++;
              }
              
              @RequiresLock("test.AnonymousClass.SuperIsNotInner.StaticContext:SF3")
              public int foo5() {
                return staticField3;
              }

              public int foo6() {
                lockSF3.lock();
                try {
                  return staticField3;
                } finally {
                  lockSF3.unlock();
                }
              }
            };
          } finally {
            lockSF3.unlock();
          }
        } finally {
          lockSF2.unlock();
        }
      } finally {
        lockSF1.unlock();
      }
    } finally {
      Super.lockCount.unlock();
    }
  }
  
  
  
  public StaticContext(final Super zuper) {
    // do nothing
  }
  
  public class Subclass extends StaticContext {
    @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT, test.AnonymousClass.SuperIsNotInner.StaticContext:SF1, test.AnonymousClass.SuperIsNotInner.StaticContext:SF2")
    public Subclass() {
      /* zuper has no immediately enclosing instance.  
       * zuper has no immediately enclosing instance with respect to Super.
       *   
       * Writes Super.count, StaticContext.staticField2; reads StaticContext.staticField1
       */
      super(new Super() {
        private int f = staticField1;
        private int g;
        
        { 
          g = staticField2++;
        }
        
        @RequiresLock("test.AnonymousClass.SuperIsNotInner.StaticContext:SF3")
        public int foo7() {
          return staticField3;
        }

        public int foo8() {
          lockSF3.lock();
          try {
            return staticField3;
          } finally {
            lockSF3.unlock();
          }
        }
      });
    }
  }
}
