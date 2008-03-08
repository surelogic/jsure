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
      
      @RegionEffects("writes test.NewExpression.MemberClass.Qualified.Outer.Middle.this:g, any(Outer):f")
      public void doStuff2() {
        Middle.this. new Inner();
      }
    }
  }
  
  @RegionEffects("writes java.lang.Object:All")
  public static void main(String args[]) {
    final Outer outer1 = new Outer();
    // 1st enclosing instance is outer1
    // Writes <outer1>.f [masked]
    final Middle middle1 = outer1. new Middle();
    // 1st enclosing instance is middle1
    // 2nd enclosing instance is middle1's first enclosing instance: outer1
    // Writes <middle1>.g [masked], any(Outer).f
    final Middle.Inner inner1 = middle1. new Inner();
    System.out.println(inner1.h + " " + middle1.g + " " + outer1.f);
    
    outer1.f = middle1.g = inner1.h = 0;
    final Outer outer2 = new Outer();
    // 1st enclosing instance is outer2
    // Writes <outer2>.f [masked]
    final Middle middle2 = outer2. new Middle();
    // 1st enclosing instance is middle2
    // 2nd enclosing instance is middle2's first enclosing instance: outer2
    // Writes <middle2>.g [masked], any(Outer).f
    final Middle.Inner inner2 = middle2. new Inner();
    outer2.f = middle2.g = inner2.h = 0;
    
    // Writes <middle1>.g [masked], any(Outer).f
    inner2.doStuff1(middle1);
    System.out.println(inner1.h + " " + middle1.g + " " + outer1.f);
    System.out.println(inner2.h + " " + middle2.g + " " + outer2.f);
    
    // Writes any(Middle).g, any(Outer).f
    inner2.doStuff2();
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
          
          @RegionEffects("writes test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.Middle.this:g, any(test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2):xx")
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
