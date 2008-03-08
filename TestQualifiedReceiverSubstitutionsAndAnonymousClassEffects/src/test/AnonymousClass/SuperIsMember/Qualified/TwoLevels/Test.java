package test.AnonymousClass.SuperIsMember.Qualified.TwoLevels;

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
      
      // Writes Test.this.t1, Container.this.c1
      @RegionEffects("writes test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test.this:t1, test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test.Container.this:c1")
      public Super() {
        // do stuff
        Test.this.t1 = 5;
        this.f = 10;
      }
    }
  
    
    
    @RegionEffects("writes other:c1, other:c2, any(test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test):t1, any(test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test):t2")
    public Container(final Container other) {
      /* The immediately enclosing instance of s1 is "this" (a Container object)
       * The immediately enclosing instance with respect to Super is other.
       * 
       * Writes any(Test).t1, any(Test).t2, other.c1, other.c2
       */
      final Super s1 = other. new Super() { 
        private int g = 10;
        { t2 += 1; }
        { c2 = 9; }
      };
    }
  
    @RegionEffects("writes other:c1, other:c2, any(test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test):t1, any(test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test):t2")
    public void stuff(Container other) {
      /* The immediately enclosing instance of s2 is "this" (a Container object)
       * The immediately enclosing instance with respect to Super is other.
       * 
       * Writes any(Test).t1, any(Test).t2, other.c1, other.c2
       */
      final Super s2 = other. new Super() { 
        private int g = 10;
        { t2 += 1; }
        { c2 = 9; }
      };
    }
  
    
    
    public class Middle1 {
      public int m1;
      
      @RegionEffects("writes other:c1, other:c2, any(test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test):t1, any(test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test):t2")
      public Middle1(Container other) {
        /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
         * The immediately enclosing instance with respect to Super is other.
         * 
         * Writes any(Test).t1, ant(Test).t2, other.c1, other.c2, this.m1
         */
        final Super s3 = other. new Super() { 
          private int g = 10;
          { m1 = 10; }
          { t2 += 1; }
          { c2 = 9; }
        };
      }
  
      @RegionEffects("writes other:c1, other:c2, any(test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test):t1, any(test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test):t2, this:m1")
      public void stuff(Container other) {
        /* The immediately enclosing instance of s4 is "this" (a Middle1 object)
         * The immediately enclosing instance with respect to Super is other.
         * 
         * Writes any(Test).t1, ant(Test).t2, other.c1, other.c2, this.m1
         */
        final Super s4 = other. new Super() { 
          private int g = 10;
          { m1 = 20; }
          { t2 += 1; }
          { c2 = 9; }
        };
      }
  
    
    
      public class Middle2 {
        public int m2;
        
        @RegionEffects("writes other:c1, other:c2, any(test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test):t1, any(test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test):t2, any(test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test.Container.Middle1):m1")
        public Middle2(Container other) {
          /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
           * The immediately enclosing instance with respect to Super is other.
           * 
           * Writes any(Test).t1, any(Test).t2, any(Middle1).m1, other.c1, other.c2, this.m2
           */
          final Super s5 = other. new Super() { 
            private int g = 10;
            { m1 = 10; }
            { m2 = 20; }
            { t2 += 1; }
            { c2 = 9; }
          };
        }
  
        @RegionEffects("writes other:c1, other:c2, any(test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test):t1, any(test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test):t2, any(test.AnonymousClass.SuperIsMember.Qualified.TwoLevels.Test.Container.Middle1):m1, this:m2")
        public void stuff(Container other) {
          /* The immediately enclosing instance of s6 is "this" (a Middle2 object)
           * The immediately enclosing instance with respect to Super is other.
           * 
           * Writes any(Test).t1, any(Test).t2, any(Middle1).m1, other.c1, other.c2, this.m2
           */
          final Super s6 = other. new Super() { 
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
