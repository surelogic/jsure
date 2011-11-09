package test.AnonymousClass.SuperIsMember.Unqualified.OneLevel;

import com.surelogic.RegionEffects;

public class Test {
  public int t1;
  public int t2;
  
  
  
  public class Super {
    public int f;
    
    @RegionEffects("writes Test.this:t1")
    public Super() {
      // do stuff
      Test.this.t1 = 5;
      this.f = 10;
    }
  }

  
  
  @RegionEffects("none")
  public Test() {
    /* The immediately enclosing instance of s1 is "this" (a Test object)
     * The immediately enclosing instance with respect to Super is Test.this.
     * 
     * Writes this.t1, this.t2
     */
    final Super s1 = new Super() { 
      private int g = 10;
      { t2 += 1; }
    };
  }

  @RegionEffects("writes t1, t2")
  public void stuff() {
    /* The immediately enclosing instance of s2 is "this" (a Test object)
     * The immediately enclosing instance with respect to Super is Test.this.
     * 
     * Writes this.t1, this.t2
     */
    final Super s2 = new Super() { 
      private int g = 10;
      { t2 += 1; }
    };
  }

  
  
  public class Middle1 {
    public int m1;
    
    @RegionEffects("writes Test.this:t1, Test.this:t2")
    public Middle1() {
      /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
       * The immediately enclosing instance with respect to Super is Test.this.
       * 
       * Writes Test.this.t1, Test.this.t2, this.m1
       */
      final Super s3 = new Super() { 
        private int g = 10;
        { m1 = 10; }
        { t2 += 1; }
      };
    }

    @RegionEffects("writes Test.this:t1, Test.this:t2, this:m1")
    public void stuff() {
      /* The immediately enclosing instance of s4 is "this" (a Middle1 object)
       * The immediately enclosing instance with respect to Super is Test.this.
       * 
       * Writes Test.this.t1, Test.this.t2, this.m1
       */
      final Super s4 = new Super() { 
        private int g = 10;
        { m1 = 20; }
        { t2 += 1; }
      };
    }

  
  
    public class Middle2 {
      public int m2;
      
      @RegionEffects("writes Test.this:t1, Test.this:t2, test.AnonymousClass.SuperIsMember.Unqualified.OneLevel.Test.Middle1.this:m1")
      public Middle2() {
        /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
         * The immediately enclosing instance with respect to Super is Test.this.
         * 
         * Writes Test.this.t1, Test.this.t2, Middle1.this.m1, this.m2
         */
        final Super s5 = new Super() { 
          private int g = 10;
          { m1 = 10; }
          { m2 = 20; }
          { t2 += 1; }
        };
      }

      @RegionEffects("writes Test.this:t1, Test.this:t2, test.AnonymousClass.SuperIsMember.Unqualified.OneLevel.Test.Middle1.this:m1, this:m2")
      public void stuff() {
        /* The immediately enclosing instance of s6 is "this" (a Middle2 object)
         * The immediately enclosing instance with respect to Super is Test.this.
         * 
         * Writes Test.this.t1, Test.this.t2, Middle.this.m1, this.m2
         */
        final Super s6 = new Super() { 
          private int g = 10;
          { m1 = 20; }
          { m2 = 30; }
          { t2 += 1; }
        };
      }
    }
  }
}
