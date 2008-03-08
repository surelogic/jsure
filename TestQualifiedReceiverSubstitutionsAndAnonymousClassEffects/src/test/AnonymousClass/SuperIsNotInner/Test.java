package test.AnonymousClass.SuperIsNotInner;

import com.surelogic.RegionEffects;

public class Test {
  public int t;
  
  @RegionEffects("writes test.AnonymousClass.SuperIsNotInner.Super:count")
  public Test() {
    /* The immediately enclosing instance of s1 is "this" (a Test object)
     * There is no immediately enclosing instance with respect to Super.
     * 
     * Writes Super.count, this.t
     */
    final Super s1 = new Super() { 
      private int g = 10;
      { Test.this.t += 1; }
    };
  }

  @RegionEffects("writes test.AnonymousClass.SuperIsNotInner.Super:count, this:t")
  public void stuff() {
    /* The immediately enclosing instance of s2 is "this" (a Test object)
     * There is no immediately enclosing instance with respect to Super.
     * 
     * Writes Super.count, this.t
     */
    final Super s2 = new Super() { 
      private int g = 10;
      { t += 1; }
    };
  }

  
  
  public class Middle1 {
    public int m1;
    
    @RegionEffects("writes test.AnonymousClass.SuperIsNotInner.Super:count, any(Test):t")
    public Middle1() {
      /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
       * There is no immediately enclosing instance with respect to Super.
       * 
       * Writes Super.count, this.m1, any(Test).t
       */
      final Super s3 = new Super() { 
        private int g = 10;
        { m1 = 10; }
        { t += 1; }
      };
    }

    @RegionEffects("writes test.AnonymousClass.SuperIsNotInner.Super:count, this:m1, any(Test):t")
    public void stuff() {
      /* The immediately enclosing instance of s4 is "this" (a Middle1 object)
       * There is no immediately enclosing instance with respect to Super.
       * 
       * Writes Super.count, this.m1, any(Test).t
       */
      final Super s4 = new Super() { 
        private int g = 10;
        { m1 = 20; }
        { t += 1; }
      };
    }

  
  
    public class Middle2 {
      public int m2;
      
      @RegionEffects("writes test.AnonymousClass.SuperIsNotInner.Super:count, any(Test):t, any(test.AnonymousClass.SuperIsNotInner.Test.Middle1):m1")
      public Middle2() {
        /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
         * There is no immediately enclosing instance with respect to Super.
         * 
         * Writes Super.count, this.m2, any(Middle1).m1, any(Test).t
         */
        final Super s5 = new Super() { 
          private int g = 10;
          { m1 = 10; }
          { m2 = 20; }
          { t += 1; }
        };
      }

      @RegionEffects("writes test.AnonymousClass.SuperIsNotInner.Super:count, this:m2, any(Test):t, any(test.AnonymousClass.SuperIsNotInner.Test.Middle1):m1")
      public void stuff() {
        /* The immediately enclosing instance of s6 is "this" (a Middle2 object)
         * There is no immediately enclosing instance with respect to Super.
         * 
         * Writes Super.count, this.m2, any(Middle1).m1, any(Test).t
         */
        final Super s6 = new Super() { 
          private int g = 10;
          { m1 = 20; }
          { m2 = 30; }
          { t += 1; }
        };
      }
    }
  }
}
