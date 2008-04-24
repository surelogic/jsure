package test.NewExpression.MemberClass.Unqualified;

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
    
    @RequiresLock("Outer.this:F")
    public void doStuff2() {
      // Writes Outer.this:f
      new Middle();
    }
    
    
    @RegionLock("H is lockH protects h")
    public class Inner {
      public final Object lockH = new Object();
      public int h;
      
      @RequiresLock("Outer.this:F, test.NewExpression.MemberClass.Unqualified.Outer.Middle.this:G")
      @SingleThreaded
      @Borrowed("this")
      public Inner() {
        this.h = 5;
        Middle.this.g = 6;
        Outer.this.f = 7;
      }
      
      @RequiresLock("Outer.this:F, test.NewExpression.MemberClass.Unqualified.Outer.Middle.this:G")
      public void doStuff2() {
        new Inner(); // F, G Assures
      }
    }
  }
  
  @RequiresLock("this:F")
  public void doStuff2() {
    new Middle();
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
        
        @RequiresLock("test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.this:XX")
        @SingleThreaded
        @Borrowed("this")
        public Middle() {
          this.g = 10;
          Outer2.this.xx = 11;
        }
               
        @RequiresLock("test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.this:XX")
        public void doStuff2() {
          new Middle();
        }
        
        @RegionLock("H is lockH protects h")
        public class Inner {
          public final Object lockH = new Object();
          public int h;
          
          @RequiresLock("test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.Middle.this:G, test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.this:XX")
          @SingleThreaded
          @Borrowed("this")
          public Inner() {
            this.h = 5;
            Middle.this.g = 6;
            Outer2.this.xx = 7;
          }
          
          @RequiresLock("test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.this:XX, test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.Middle.this:G")
          public void doStuff2() {
            new Inner(); // G, XX assures
          }
        }
      }
      
      @RequiresLock("this:XX")
      public void doStuff2() {
        new Middle();
      }
    }
  }
}
