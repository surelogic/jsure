package test.AnonymousClass.SuperIsMember.Unqualified.OneLevel;

import com.surelogic.RegionEffects;

public class TestFieldInit {
  public int t1;
  public int t2;
  
  
  
  public class Super {
    public int f;
    
    // Writes TestFieldInit.this.t1
    @RegionEffects("writes TestFieldInit.this:t1")
    public Super() {
      // do stuff
      TestFieldInit.this.t1 = 5;
      this.f = 10;
    }
  }

  
  
  
  /* The immediately enclosing instance of s1 is "this" (a Test object)
   * The immediately enclosing instance with respect to Super is this
   * 
   * Writes this.t1, this.t2
   */
  final Super s1  = new Super() { 
    private int g = 10;
    { t2 += 1; }
  };
  
  @RegionEffects("none")
  public TestFieldInit() {
    /* do nothing, but initializer for s1 is analyzed.
     * Writes this.t1, this.t2, both of which are masked.
     */
    
  }



  public class Middle1 {
    public int m1;
    
    /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
     * The immediately enclosing instance with respect to Super is TestFieldInit.this
     * 
     * Writes TestFieldInit.this.t1, TestFieldInit.this.t2, this.m1
     */
    final Super s3 = new Super() { 
      private int g = 10;
      { m1 = 10; }
      { t2 += 1; }
    };      
    
    @RegionEffects("writes TestFieldInit.this:t1, TestFieldInit.this:t2")
    public Middle1() {
      /* do nothing, but initializer for s3 is analyzed.
       * writes TestFieldInit.this.t1, TestFieldInit.this.t2, this.m1,
       * but this.m1 is masked.
       */
    }

  
  
    public class Middle2 {
      public int m2;

      /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
       * The immediately enclosing instance with respect to Super is TestFieldInit.this
       * 
       * Writes TestFieldInit.this.t1, TestFieldInit.this.t1, Middle1.this.m1, this.m2
       */
      final Super s5 = new Super() { 
        private int g = 10;
        { m1 = 10; }
        { m2 = 20; }
        { t2 += 1; }
      };        
      
      @RegionEffects("writes TestFieldInit.this:t1, TestFieldInit.this:t2, test.AnonymousClass.SuperIsMember.Unqualified.OneLevel.TestFieldInit.Middle1.this:m1")
      public Middle2() {
        // do nothing, but initializer for s5 is analyzed
      }
    }
  }
}
