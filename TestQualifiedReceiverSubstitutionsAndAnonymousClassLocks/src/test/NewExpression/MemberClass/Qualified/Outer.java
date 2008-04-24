package test.NewExpression.MemberClass.Qualified;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.SingleThreaded;

@RegionLock("F is lockF protects f")
public class Outer {
  public final Object lockF = new Object();
  public int f;
  
  @RegionLock("G is lockG protects g")
  public class Middle {
    public final Object lockG = new Object();
    public int g;
    
    @RequiresLock("Outer.this:F")
    @SingleThreaded
    @Borrowed("this")
    public Middle() {
      this.g = 10;
      Outer.this.f = 11;
    }
    
    @RequiresLock("a:F")
    public void doStuff1(final Outer a) {
      a. new Middle();
    }
    
    @RequiresLock("Outer.this:F")
    public void doStuff2() {
      Outer.this. new Middle();
    }
    
    
    
    @RegionLock("H is lockH protects h")
    public class Inner {
      public final Object lockH = new Object();
      public int h;
      
      @RequiresLock("Outer.this:F, test.NewExpression.MemberClass.Qualified.Outer.Middle.this:G")
      @SingleThreaded
      @Borrowed("this")
      public Inner() {
        this.h = 5;
        Middle.this.g = 6;
        Outer.this.f = 7;
      }
      
      @RequiresLock("m:G")
      public void doStuff1(final Middle m) {
        /* 1st enclosing instance will be m
         * 2nd enclosing instance will be m's first enclosing instance
         */ 
        m. new Inner(); // G assures, but F is unresolvable
      }
      
      @RequiresLock("Outer.this:F, test.NewExpression.MemberClass.Qualified.Outer.Middle.this:G")
      public void doStuff2() {
        Middle.this. new Inner(); // F, G assures
      }
    }
  }
  
  @RequiresLock("a:F")
  public void doStuff1(final Outer a) {
    a. new Middle();
  }
  
  @RequiresLock("this:F")
  public void doStuff2() {
    this. new Middle();
  }
  
  @RequiresLock("a:G")
  public void doStuff3(final Middle a) {
    a. new Inner(); // G assures, but F is unresolvable
  }
  
  

  public static class Static1 {
    // no fields
    
    @RegionLock("XX is lockXX protects xx")
    public class Outer2 {
      public final Object lockXX = new Object();
      public int xx;

      @RegionLock("G is lockG protects g")
      public class Middle {
        public final Object lockG = new Object();
        public int g;
        
        @RequiresLock("test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.this:XX")
        @SingleThreaded
        @Borrowed("this")
        public Middle() {
          this.g = 10;
          Outer2.this.xx = 11;
        }
        
        @RequiresLock("a:XX")
        public void doStuff1(final Outer2 a) {
          a. new Middle();
        }
        
        @RequiresLock("test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.this:XX")
        public void doStuff2() {
          Outer2.this. new Middle();
        }
        
        @RegionLock("H is lockH protects h")
        public class Inner {
          public final Object lockH = new Object();
          public int h;
          
          @RequiresLock("test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.Middle.this:G, test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.this:XX")
          @SingleThreaded
          @Borrowed("this")
          public Inner() {
            this.h = 5;
            Middle.this.g = 6;
            Outer2.this.xx = 7;
          }
          
          @RequiresLock("m:G")
          public void doStuff1(final Middle m) {
            m. new Inner(); // G assures, XX is unresolvable
          }
          
          @RequiresLock("test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.this:XX, test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.Middle.this:G")
          public void doStuff2() {
            Middle.this. new Inner(); // XX, G assures
          }
        }
      }
      
      @RequiresLock("a:XX")
      public void doStuff1(final Outer2 a) {
        a. new Middle();
      }
      
      @RequiresLock("this:XX")
      public void doStuff2() {
        this. new Middle();
      }
      
      @RequiresLock("a:G")
      public void doStuff3(final Middle a) {
        a. new Inner(); // G assures, but XX is unresolvable
      }
    }
  }
}
