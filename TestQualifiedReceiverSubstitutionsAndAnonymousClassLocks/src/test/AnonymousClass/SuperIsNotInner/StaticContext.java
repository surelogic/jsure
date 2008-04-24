package test.AnonymousClass.SuperIsNotInner;

import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.RequiresLock;

@RegionLocks({
  @RegionLock("SF1 is lockSF1 protects staticField1"),
  @RegionLock("SF2 is lockSF2 protects staticField2"),
  @RegionLock("SF3 is lockSF3 protects staticField3")
})
public class StaticContext {
  public static final Object lockSF1 = new Object();
  public static final Object lockSF2 = new Object();
  public static final Object lockSF3 = new Object();
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
  };
  
  
  
  
  static {
    /* s2 has no immediately enclosing instance.  
     * s2 has no immediately enclosing instance with respect to Super.
     */
    /* Needs SF1, SF2, and COUNT.  Holds SF1 and SF2 (and SF3) by virtue of being
     * part of the class initialization.  Need to explicitly acquire COUNT.
     */
    synchronized (Super.lockCount) {
      final Super s2 = new Super() {
        private int f = staticField1;
        private int g;
        
        { 
          g = staticField2++;
        }
        
        @RequiresLock("test.AnonymousClass.SuperIsNotInner.StaticContext:SF3")
        public int foo2() {
          return staticField3;
        }
      };
    }
  }
  
  
  
  
  @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT, test.AnonymousClass.SuperIsNotInner.StaticContext:SF1, test.AnonymousClass.SuperIsNotInner.StaticContext:SF2")
  public static void doStuff() {
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
      public int foo3() {
        return staticField3;
      }
    };
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
        public int foo1() {
          return staticField3;
        }
      });
    }
  }
}
