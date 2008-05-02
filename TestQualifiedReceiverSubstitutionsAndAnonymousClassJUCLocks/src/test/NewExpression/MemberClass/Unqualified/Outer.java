package test.NewExpression.MemberClass.Unqualified;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;
import com.surelogic.SingleThreaded;

@RegionLock("F is lockF protects f")
public class Outer {
  public final Lock lockF = new ReentrantLock();
  public int f;
  
  @RegionLock("G is lockG protects g")
  public class Middle {
    public final Lock lockG = new ReentrantLock();
    public int g;
    
    @RequiresLock("Outer.this:F")
    @SingleThreaded
    @Borrowed("this")
    public Middle() {
      this.g = 10;
      Outer.this.f = 11;
    }
    
    public void doStuff() {
      Outer.this.lockF.lock();
      try {
        new Middle();
      } finally {
        Outer.this.lockF.unlock();
      }
    }

    @RequiresLock("Outer.this:F")
    public void doStuff2() {
      // Writes Outer.this:f
      new Middle();
    }
    
    
    @RegionLock("H is lockH protects h")
    public class Inner {
      public final Lock lockH = new ReentrantLock();
      public int h;
      
      @RequiresLock("Outer.this:F, test.NewExpression.MemberClass.Unqualified.Outer.Middle.this:G")
      @SingleThreaded
      @Borrowed("this")
      public Inner() {
        this.h = 5;
        Middle.this.g = 6;
        Outer.this.f = 7;
      }
      
      public void doStuff() {
        Outer.this.lockF.lock();
        try {
          Middle.this.lockG.lock();
          try {
            new Inner();
          } finally {
            Middle.this.lockG.unlock();
          }
        } finally {
          Outer.this.lockF.lock();
        }
      }
      
      @RequiresLock("Outer.this:F, test.NewExpression.MemberClass.Unqualified.Outer.Middle.this:G")
      public void doStuff2() {
        new Inner(); // F, G Assures
      }
    }
  }
  
  public void doStuff() {
    this.lockF.lock();
    try {
      new Middle();
    } finally {
      this.lockF.unlock();
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
      public final Lock lockXX = new ReentrantLock();
      public int xx;

      @RegionLock("G is lockG protects g")
      public class Middle {
        public final Lock lockG = new ReentrantLock();
        public int g;
        
        @RequiresLock("test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.this:XX")
        @SingleThreaded
        @Borrowed("this")
        public Middle() {
          this.g = 10;
          Outer2.this.xx = 11;
        }

        @RequiresLock("test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.this:XX")
        public void doStuff() {
          Outer2.this.lockXX.lock();
          try {
            new Middle();
          } finally {
            Outer2.this.lockXX.unlock();
          }
        }

        @RequiresLock("test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.this:XX")
        public void doStuff2() {
          new Middle();
        }
        
        @RegionLock("H is lockH protects h")
        public class Inner {
          public final Lock lockH = new ReentrantLock();
          public int h;
          
          @RequiresLock("test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.Middle.this:G, test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.this:XX")
          @SingleThreaded
          @Borrowed("this")
          public Inner() {
            this.h = 5;
            Middle.this.g = 6;
            Outer2.this.xx = 7;
          }

          public void doStuff() {
            Outer2.this.lockXX.lock();
            try {
              Middle.this.lockG.lock();
              try {
                new Inner();
              } finally {
                Middle.this.lockG.unlock();
              }
            } finally {
              Outer2.this.lockXX.unlock();
            }
          }
          
          @RequiresLock("test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.this:XX, test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.Middle.this:G")
          public void doStuff2() {
            new Inner(); // G, XX assures
          }
        }
      }

      @RequiresLock("test.NewExpression.MemberClass.Unqualified.Outer.Static1.Outer2.this:XX")
      public void doStuff() {
        this.lockXX.lock();
        try {
          new Middle();
        } finally {
          this.lockXX.unlock();
        }
      }

      @RequiresLock("this:XX")
      public void doStuff2() {
        new Middle();
      }
    }
  }
}
