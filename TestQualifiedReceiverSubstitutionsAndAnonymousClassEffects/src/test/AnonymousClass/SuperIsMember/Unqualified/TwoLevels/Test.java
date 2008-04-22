package test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels;

import com.surelogic.RegionEffects;

public class Test {
  public int t1;
  public int t2;
  
  public class Container {
    public int c1;
    public int c2;
    
    public class Super {
      public int f;
      
      { 
        Container.this.c1 = 10;
      }
      
      @RegionEffects("writes Test.this:t1, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.Test.Container.this:c1")
      public Super() {
        // do stuff
        Test.this.t1 = 5;
        this.f = 10;
      }
    }
  
    
    
    @RegionEffects("writes Test.this:t1, Test.this:t2")
    public Container() {
      /* The immediately enclosing instance of s1 is "this" (a Container object)
       * The immediately enclosing instance with respect to Super is Container.this.
       * 
       * Writes Test.this.t1, Test.this.t2, this.c1, this.c2
       */
      final Super s1 = new Super() { 
        private int g = 10;
        { t2 += 1; }
        { c2 = 9; }
      };
    }
  
    @RegionEffects("writes Test.this:t1, Test.this:t2, this:c1, this:c2")
    public void stuff() {
      /* The immediately enclosing instance of s2 is "this" (a Container object)
       * The immediately enclosing instance with respect to Super is Container.this.
       * 
       * Writes Test.this.t1, Test.this.t2, this.c1, this.c2
       */
      final Super s2 = new Super() { 
        private int g = 10;
        { t2 += 1; }
        { c2 = 9; }
      };
    }
  
    
    
    public class Middle1 {
      public int m1;
      
      @RegionEffects("writes Test.this:t1, Test.this:t2, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.Test.Container.this:c1, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.Test.Container.this:c2")
      public Middle1() {
        /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
         * The immediately enclosing instance with respect to Super is Container.this.
         * 
         * Writes Test.this.t1, Test.this.t2, Container.this.c1, Container.this.c2, this.m1
         */
        final Super s3 = new Super() { 
          private int g = 10;
          { m1 = 10; }
          { t2 += 1; }
          { c2 = 9; }
        };
      }
  
      @RegionEffects("writes Test.this:t1, Test.this:t2, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.Test.Container.this:c1, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.Test.Container.this:c2, this:m1")
      public void stuff() {
        /* The immediately enclosing instance of s4 is "this" (a Middle1 object)
         * The immediately enclosing instance with respect to Super is Container.this.
         * 
         * Writes Test.this:t1, Test.this:t2, Container.this.c1, Container.this.c2, this.m1
         */
        final Super s4 = new Super() { 
          private int g = 10;
          { m1 = 20; }
          { t2 += 1; }
          { c2 = 9; }
        };
      }
  
    
    
      public class Middle2 {
        public int m2;
        
        @RegionEffects("writes Test.this:t1, Test.this:t2, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.Test.Container.this:c1, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.Test.Container.this:c2, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.Test.Container.Middle1.this:m1")
        public Middle2() {
          /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
           * The immediately enclosing instance with respect to Super is Container.this.
           * 
           * Writes Test.this:t1, Test.this:t2, Middle1.this.m1, Container.this.c1, Container.this.c2, this.m2
           */
          final Super s5 = new Super() { 
            private int g = 10;
            { m1 = 10; }
            { m2 = 20; }
            { t2 += 1; }
            { c2 = 9; }
          };
        }
  
        @RegionEffects("writes Test.this:t1, Test.this:t2, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.Test.Container.this:c1, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.Test.Container.this:c2, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.Test.Container.Middle1.this:m1, this:m2")
        public void stuff() {
          /* The immediately enclosing instance of s6 is "this" (a Middle2 object)
           * The immediately enclosing instance with respect to Super is Container.this.
           * 
           * Writes Test.this:t1, Test.this:t2, Middle1.this.m1, Container.this.c1, Container.this.c2, this.m2
           */
          final Super s6 = new Super() { 
            private int g = 10;
            { m1 = 20; }
            { m2 = 30; }
            { t2 += 1; }
            { c2 = 9; }
          };
        }
      }
    }
  }
}
