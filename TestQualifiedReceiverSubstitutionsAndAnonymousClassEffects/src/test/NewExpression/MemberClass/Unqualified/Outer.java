package test.NewExpression.MemberClass.Unqualified;

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
    
    @RegionEffects("writes Outer.this:f")
    public void doStuff2() {
      // Writes Outer.this:f
      new Middle();
    }
    
    
    
    public class Inner {
      public int h;
      
      @RegionEffects("writes Outer.this:f, test.NewExpression.MemberClass.Unqualified.Outer.Middle.this:g")
      public Inner() {
        this.h = 5;
        Middle.this.g = 6;
        Outer.this.f = 7;
      }
      
      @RegionEffects("writes test.NewExpression.MemberClass.Unqualified.Outer.Middle.this:g, any(Outer):f")
      public void doStuff2() {
        new Inner();
      }
    }
  }
  
  @RegionEffects("writes this:f")
  public void doStuff2() {
    new Middle();
  }

  
  

  public static class Static1 {
    // no fields
    
    public class Outer2 {
      // no fields
      public int xx;

      public class Middle {
        public int g;
        
        @RegionEffects("writes test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.this:xx")
        public Middle() {
          this.g = 10;
          Outer2.this.xx = 11;
        }
               
        @RegionEffects("writes test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.this:xx")
        public void doStuff2() {
          new Middle();
        }
        
        public class Inner {
          public int h;
          
          @RegionEffects("writes test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.Middle.this:g, test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.this:xx")
          public Inner() {
            this.h = 5;
            Middle.this.g = 6;
            Outer2.this.xx = 7;
          }
          
          @RegionEffects("writes test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.Middle.this:g, any(test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2):xx")
          public void doStuff2() {
            new Inner();
          }
        }
      }
      
      @RegionEffects("writes this:xx")
      public void doStuff2() {
        new Middle();
      }
    }
  }
}
