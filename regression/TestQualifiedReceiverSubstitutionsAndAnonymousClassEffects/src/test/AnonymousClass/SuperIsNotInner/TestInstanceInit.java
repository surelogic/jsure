package test.AnonymousClass.SuperIsNotInner;

import com.surelogic.RegionEffects;

public class TestInstanceInit {
  public int t;
  
  {
    /* The immediately enclosing instance of s1 is "this" (a Test object)
     * There is no immediately enclosing instance with respect to Super.
     * 
     * Writes Super.count, this.t
     */
    final Super s1 = new Super() { 
      private int g = 10;
      { t += 1; }
    };
  }
  
  @RegionEffects("writes test.AnonymousClass.SuperIsNotInner.Super:count")
  public TestInstanceInit() {
    /* do nothing, but initializer for s1 is analyzed.  Effects from initializer
     * are writes Super.count, this.t.  Effect this.t is masked.
     */
  }



  public class Middle1 {
    public int m1;
    
    {
      /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
       * There is no immediately enclosing instance with respect to Super.
       * 
       * Writes Super.count, this.m1, TestInstanceInit.this.t
       */
      final Super s3 = new Super() { 
        private int g = 10;
        { m1 = 10; }
        { t += 1; }
      };      
    }
    
    @RegionEffects("writes test.AnonymousClass.SuperIsNotInner.Super:count, TestInstanceInit.this:t")
    public Middle1() {
      /* Do nothing, but initializer for s3 is analyzed.  It has the effects
       * writes Super.count, this.m1, TestInstanceInit.this.t.  The effect this.m1 
       * is masked.
       */
    }

  
  
    public class Middle2 {
      public int m2;

      {
        /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
         * There is no immediately enclosing instance with respect to Super.
         * 
         * Writes Super.count, this.m2, Middle1.this.m1, TestInstanceInit.this.t
         */
        final Super s5 = new Super() { 
          private int g = 10;
          { m1 = 10; }
          { m2 = 20; }
          { t += 1; }
        };        
      }
      
      @RegionEffects("writes test.AnonymousClass.SuperIsNotInner.Super:count, TestInstanceInit.this:t, test.AnonymousClass.SuperIsNotInner.TestInstanceInit.Middle1.this:m1")
      public Middle2() {
        /* do nothing, but initializer for s5 is analyzed.  It has the effects
         * writes Super.count, this.m2, Middle1.this.m1, TestInstanceInit.this.t.
         * The effect this.m2 is masked.
         */
      }
    }
  }
}
