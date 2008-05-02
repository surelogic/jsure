package test.NewExpression.MemberClass.Qualified;

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
    
    @RequiresLock("a:F")
    public void doStuff1a(final Outer a) {
      a. new Middle();
    }
    
    public void doStuff1b(final Outer a) {
      a.lockF.lock();
      try {
        a. new Middle();
      } finally {
        a.lockF.unlock();
      }
    }
    
    @RequiresLock("Outer.this:F")
    public void doStuff2a() {
      Outer.this. new Middle();
    }
    
    public void doStuff2b() {
      Outer.this.lockF.lock();
      try {
        Outer.this. new Middle();
      } finally {
        Outer.this.lockF.unlock();
      }
    }
    
    
    
    @RegionLock("H is lockH protects h")
    public class Inner {
      public final Lock lockH = new ReentrantLock();
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
      public void doStuff1a(final Middle m) {
        /* 1st enclosing instance will be m
         * 2nd enclosing instance will be m's first enclosing instance
         */ 
        m. new Inner(); // G assures, but F is unresolvable
      }

      public void doStuff1b(final Middle m) {
        /* 1st enclosing instance will be m
         * 2nd enclosing instance will be m's first enclosing instance
         */
        m.lockG.lock();
        try {
          m. new Inner(); // G assures, but F is unresolvable
        } finally {
          m.lockG.unlock();
        }
      }

      @RequiresLock("Outer.this:F, test.NewExpression.MemberClass.Qualified.Outer.Middle.this:G")
      public void doStuff2a() {
        Middle.this. new Inner(); // F, G assures
      }

      public void doStuff2b() {
        Outer.this.lockF.lock();
        try {
          Middle.this.lockG.lock();
          try {
            Middle.this. new Inner(); // F, G assures
          } finally {
            Middle.this.lockG.unlock();
          }
        } finally {
          Outer.this.lockF.unlock();
        }
      }
    }
  }
  
  @RequiresLock("a:F")
  public void doStuff1(final Outer a) {
    a. new Middle();
  }
  
  public void doStuff1a(final Outer a) {
    a.lockF.lock();
    try {
      a. new Middle();
    } finally {
      a.lockF.unlock();
    }
  }
  
  @RequiresLock("this:F")
  public void doStuff2a() {
    this. new Middle();
  }

  public void doStuff2b() {
    this.lockF.lock();
    try {
      this. new Middle();
    } finally {
      this.lockF.unlock();
    }
  }
  
  @RequiresLock("a:G")
  public void doStuff3a(final Middle a) {
    a. new Inner(); // G assures, but F is unresolvable
  }
  
  public void doStuff3b(final Middle a) {
    a.lockG.lock();
    try {
      a. new Inner(); // G assures, but F is unresolvable
    } finally {
      a.lockG.unlock();
    }
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
        
        @RequiresLock("test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.this:XX")
        @SingleThreaded
        @Borrowed("this")
        public Middle() {
          this.g = 10;
          Outer2.this.xx = 11;
        }
        
        @RequiresLock("a:XX")
        public void doStuff1a(final Outer2 a) {
          a. new Middle();
        }

        public void doStuff1b(final Outer2 a) {
          a.lockXX.lock();
          try {
            a. new Middle();
          } finally {
            a.lockXX.unlock();
          }
        }

        @RequiresLock("test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.this:XX")
        public void doStuff2a() {
          Outer2.this. new Middle();
        }

        public void doStuff2b() {
          Outer2.this.lockXX.lock();
          try {
            Outer2.this. new Middle();  
          } finally {
            Outer2.this.lockXX.unlock();
          }
        }

        @RegionLock("H is lockH protects h")
        public class Inner {
          public final Lock lockH = new ReentrantLock();
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
          public void doStuff1a(final Middle m) {
            m. new Inner(); // G assures, XX is unresolvable
          }
          
          public void doStuff1b(final Middle m) {
            m.lockG.lock();
            try {
              m. new Inner(); // G assures, XX is unresolvable
            } finally {
              m.lockG.unlock();
            }
          }
          
          @RequiresLock("test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.this:XX, test.NewExpression.MemberClass.Qualified.Outer.Static1.Outer2.Middle.this:G")
          public void doStuff2a() {
            Middle.this. new Inner(); // XX, G assures
          }

          public void doStuff2b() {
            Outer2.this.lockXX.lock();
            try {
              Middle.this.lockG.lock();
              try {
                Middle.this. new Inner(); // XX, G assures
              } finally {
                Middle.this.lockG.unlock();
              }
            } finally {
              Outer2.this.lockXX.unlock();
            }
          }
        }
      }
      
      @RequiresLock("a:XX")
      public void doStuff1a(final Outer2 a) {
        a. new Middle();
      }
      
      public void doStuff1b(final Outer2 a) {
        a.lockXX.lock();
        try {
          a. new Middle();
        } finally {
          a.lockXX.unlock();
        }
      }
      
      @RequiresLock("this:XX")
      public void doStuff2a() {
        this. new Middle();
      }
      
      @RequiresLock("this:XX")
      public void doStuff2b() {
        this.lockXX.lock();
        try {
          this. new Middle();
        } finally {
          this.lockXX.unlock();
        }
      }
      
      @RequiresLock("a:G")
      public void doStuff3a(final Middle a) {
        a. new Inner(); // G assures, but XX is unresolvable
      }

      public void doStuff3b(final Middle a) {
        a.lockG.lock();
        try {
          a. new Inner(); // G assures, but XX is unresolvable
        } finally {
          a.lockG.unlock();
        }
      }
    }
  }
}
