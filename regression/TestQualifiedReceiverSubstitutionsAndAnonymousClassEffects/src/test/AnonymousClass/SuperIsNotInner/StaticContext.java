package test.AnonymousClass.SuperIsNotInner;

import com.surelogic.RegionEffects;

public class StaticContext {
  public static int staticField1 = 100;
  public static int staticField2 = 200;
  public static int staticField3 = 300;
  
  public int nonStaticField = 1;
  
  
  
  
  /* s1 has no immediately enclosing instance.  
   * s1 has no immediately enclosing instance with respect to Super.
   * 
   * Writes Super.count, StaticContext.staticField2; reads StaticContext.staticField1
   */
  public static Super s1 = new Super() {
    private int f = staticField1;
    private int g;
    
    { 
      g = staticField2++;
    }
    
    @RegionEffects("reads test.AnonymousClass.SuperIsNotInner.StaticContext:staticField3")
    public int foo1() {
      return staticField3;
    }
  };
  
  
  
  
  static {
    /* s2 has no immediately enclosing instance.  
     * s2 has no immediately enclosing instance with respect to Super.
     * 
     * Writes Super.count, StaticContext.staticField2; reads StaticContext.staticField1
     */
    final Super s2 = new Super() {
      private int f = staticField1;
      private int g;
      
      { 
        g = staticField2++;
      }
      
      @RegionEffects("reads test.AnonymousClass.SuperIsNotInner.StaticContext:staticField3")
      public int foo2() {
        return staticField3;
      }
    };
  }
  
  
  
  
  @RegionEffects("writes test.AnonymousClass.SuperIsNotInner.Super:count, test.AnonymousClass.SuperIsNotInner.StaticContext:staticField1, test.AnonymousClass.SuperIsNotInner.StaticContext:staticField2")
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
      
      @RegionEffects("reads test.AnonymousClass.SuperIsNotInner.StaticContext:staticField3")
      public int foo3() {
        return staticField3;
      }
    };
  }

  
  
  
  @RegionEffects("none")
  public StaticContext(final Super zuper) {
    // do nothing
  }
  
  public class Subclass extends StaticContext {
    @RegionEffects("writes test.AnonymousClass.SuperIsNotInner.Super:count, test.AnonymousClass.SuperIsNotInner.StaticContext:staticField1, test.AnonymousClass.SuperIsNotInner.StaticContext:staticField2")
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
        
        @RegionEffects("reads test.AnonymousClass.SuperIsNotInner.StaticContext:staticField3")
        public int foo1() {
          return staticField3;
        }
      });
    }
  }
}
