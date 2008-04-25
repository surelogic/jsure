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
    
    @RegionEffects("writes test.AnonymousClass.SuperIsNotInner.Super:count, Test.this:t")
    public Middle1() {
      /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
       * There is no immediately enclosing instance with respect to Super.
       * 
       * Writes Super.count, this.m1, Test.this.t
       */
      final Super s3 = new Super() { 
        private int g = 10;
        { m1 = 10; }
        { t += 1; }
      };
    }

    @RegionEffects("writes test.AnonymousClass.SuperIsNotInner.Super:count, this:m1, Test.this:t")
    public void stuff() {
      /* The immediately enclosing instance of s4 is "this" (a Middle1 object)
       * There is no immediately enclosing instance with respect to Super.
       * 
       * Writes Super.count, this.m1, Test.this.t
       */
      final Super s4 = new Super() { 
        private int g = 10;
        { m1 = 20; }
        { t += 1; }
      };
    }

  
  
  }
}
