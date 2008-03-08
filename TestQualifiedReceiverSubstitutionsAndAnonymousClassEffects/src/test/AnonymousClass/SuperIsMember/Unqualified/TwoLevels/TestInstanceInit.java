package test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels;

import com.surelogic.RegionEffects;

public class TestInstanceInit {
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
      
      @RegionEffects("writes TestInstanceInit.this:t1, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.TestInstanceInit.Container.this:c1")
      public Super() {
        // do stuff
        TestInstanceInit.this.t1 = 5;
        this.f = 10;
      }
    }
  

    
    {
      /* The immediately enclosing instance of s1 is "this" (a Container object)
       * The immediately enclosing instance with respect to Super is Container.this.
       * 
       * Writes any(TestInstanceInit).t1, any(TestInstanceInit).t2, this.c1, this.c2
       */
      final Super s1 = new Super() { 
        private int g = 10;
        { t2 += 1; }
        { c2 = 9; }
      };
    }
    
    @RegionEffects("writes any(TestInstanceInit):t1, any(TestInstanceInit):t2")
    public Container() {
      // Effects from instance init
    }
  
    
    
    public class Middle1 {
      public int m1;
      
      {
        /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
         * The immediately enclosing instance with respect to Super is Container.this.
         * 
         * Writes any(TestInstanceInit).t1, ant(TestInstanceInit).t2, Container.this.c1, Container.this.c2, this.m1
         */
        final Super s3 = new Super() { 
          private int g = 10;
          { m1 = 10; }
          { t2 += 1; }
          { c2 = 9; }
        };
      }
      
      @RegionEffects("writes any(TestInstanceInit):t1, any(TestInstanceInit):t2, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.TestInstanceInit.Container.this:c1, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.TestInstanceInit.Container.this:c2")
      public Middle1() {
        // Effects from instance init
      }
  
    
    
      public class Middle2 {
        public int m2;
        
        {
          /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
           * The immediately enclosing instance with respect to Super is Container.this.
           * 
           * Writes any(TestInstanceInit).t1, any(TestInstanceInit).t2, any(Middle1).m1, Container.this.c1, Container.this.c2, this.m2
           */
          final Super s5 = new Super() { 
            private int g = 10;
            { m1 = 10; }
            { m2 = 20; }
            { t2 += 1; }
            { c2 = 9; }
          };
        }
        
        @RegionEffects("writes any(TestInstanceInit):t1, any(TestInstanceInit):t2, any(test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.TestInstanceInit.Container.Middle1):m1, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.TestInstanceInit.Container.this:c1, test.AnonymousClass.SuperIsMember.Unqualified.TwoLevels.TestInstanceInit.Container.this:c2")
        public Middle2() {
          // effects from instance init
        }
      }
    }
  }
}
