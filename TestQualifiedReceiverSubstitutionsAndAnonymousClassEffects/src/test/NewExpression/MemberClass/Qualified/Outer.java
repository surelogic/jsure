package test.NewExpression.MemberClass.Qualified;

import com.surelogic.RegionEffects;

public class Outer {
  public int f;
  
  public class Middle {
    public int g;
    
    @RegionEffects("writes Outer.this:f")
    public Middle() {
      this.g = 10;
      Outer.this.f = 11;
    }
    
    @RegionEffects("writes a:f")
    public void doStuff1(final Outer a) {
      a. new Middle();
    }
    
    @RegionEffects("writes Outer.this:f")
    public void doStuff2() {
      Outer.this. new Middle();
    }
    
    
    
    public class Inner {
      public int h;
      
      @RegionEffects("writes Outer.this:f, test.NewExpression.MemberClass.Qualified.Outer.Middle.this:g")
      public Inner() {
        this.h = 5;
        Middle.this.g = 6;
        Outer.this.f = 7;
      }
      
      @RegionEffects("writes m:g, any(Outer):f")
      public void doStuff1(final Middle m) {
        /* 1st enclosing instance will be m
         * 2nd enclosing instance will be m's first enclosing instance
         */ 
        m. new Inner();
      }
      
      @RegionEffects("writes test.NewExpression.MemberClass.Qualified.Outer.Middle.this:g, Outer.this:f")
      public void doStuff2() {
        // Unlike the above, here we do NOT get an any-instance effect
        Middle.this. new Inner();
      }
    }
  }
    
  @RegionEffects("writes a:f")
  public void doStuff1(final Outer a) {
    a. new Middle();
  }
  
  @RegionEffects("writes this:f")
  public void doStuff2() {
    this. new Middle();
  }
  
  @RegionEffects("writes a:g, any(Outer):f")
  public void doStuff3(final Middle a) {
    a. new Inner();
  }
  
  

  public static class Static1 {
    // no fields
    
    public class Outer2 {
      // no fields
      public int xx;

      public class Middle {
        public int g;
        
        @RegionEffects("writes test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.this:xx")
        public Middle() {
          this.g = 10;
          Outer2.this.xx = 11;
        }
        
        @RegionEffects("writes a:xx")
        public void doStuff1(final Outer2 a) {
          a. new Middle();
        }
        
        @RegionEffects("writes test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.this:xx")
        public void doStuff2() {
          Outer2.this. new Middle();
        }
        
        public class Inner {
          public int h;
          
          @RegionEffects("writes test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.Middle.this:g, test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.this:xx")
          public Inner() {
            this.h = 5;
            Middle.this.g = 6;
            Outer2.this.xx = 7;
          }
          
          @RegionEffects("writes m:g, any(test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2):xx")
          public void doStuff1(final Middle m) {
            m. new Inner();
          }
          
          @RegionEffects("writes test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.Middle.this:g, test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.this:xx")
          public void doStuff2() {
            Middle.this. new Inner();
          }
        }
      }
      
      @RegionEffects("writes a:xx")
      public void doStuff1(final Outer2 a) {
        a. new Middle();
      }
      
      @RegionEffects("writes this:xx")
      public void doStuff2() {
        this. new Middle();
      }
      
      @RegionEffects("writes a:g, any(test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2):xx")
      public void doStuff3(final Middle a) {
        a. new Inner();
      }
    }
  }
}
