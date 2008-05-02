package test.AnonymousClass.SuperIsNotInner;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;

@RegionLock("T is lockT protects t")
public class TestInstanceInit {
  public final Lock lockT = new ReentrantLock();
  public int t;
  
  {
    /* The immediately enclosing instance of s1 is "this" (a Test object)
     * There is no immediately enclosing instance with respect to Super.
     * 
     * Writes Super.count, this.t
     */
    lockT.lock();
    try {
      final Super s1 = new Super() { 
        private int g = 10;
        { t += 1; }
      };
    } finally {
      lockT.unlock();
    }
  }
  
  @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT")
  public TestInstanceInit() {
    /* do nothing, but initializer for s1 is analyzed.  Effects from initializer
     * are writes Super.count, this.t.  Effect this.t is masked.
     */
  }



  @RegionLock("M1 is lockM1 protects m1")
  public class Middle1 {
    public final Lock lockM1 = new ReentrantLock();
    public int m1;
    
    {
      /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
       * There is no immediately enclosing instance with respect to Super.
       * 
       * Writes Super.count, this.m1, TestInstanceInit.this.t
       */
      lockM1.lock();
      try {
        final Super s3 = new Super() { 
          private int g = 10;
          { m1 = 10; }
          { t += 1; }
        };      
      } finally {
        lockM1.unlock();
      }
    }
    
    @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT, TestInstanceInit.this:T")
    public Middle1() {
      /* Do nothing, but initializer for s3 is analyzed.  It has the effects
       * writes Super.count, this.m1, TestInstanceInit.this.t.  The effect this.m1 
       * is masked.
       */
    }

  
  
    @RegionLock("M2 is lockM2 protects m2")
    public class Middle2 {
      public final Lock lockM2 = new ReentrantLock();
      public int m2;

      {
        /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
         * There is no immediately enclosing instance with respect to Super.
         * 
         * Writes Super.count, this.m2, Middle1.this.m1, TestInstanceInit.this.t
         */
        lockM2.lock();
        try {
          final Super s5 = new Super() { 
            private int g = 10;
            { m1 = 10; }
            { m2 = 20; }
            { t += 1; }
          };
        } finally {
          lockM2.unlock();
        }
      }
      
      @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT, TestInstanceInit.this:T, test.AnonymousClass.SuperIsNotInner.TestInstanceInit.Middle1.this:M1")
      public Middle2() {
        /* do nothing, but initializer for s5 is analyzed.  It has the effects
         * writes Super.count, this.m2, Middle1.this.m1, TestInstanceInit.this.t.
         * The effect this.m2 is masked.
         */
      }
    }
  }
}
