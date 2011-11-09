package test.AnonymousClass.SuperIsNotInner;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;

@RegionLock("T is lockT protects t")
public class Test {
  public final Lock lockT = new ReentrantLock();
  public int t;
  
  @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT")
  public Test() {
    /* I cannot declare this constructor to be singled threaded because
     * "this" is aliased by the creation of an anonymous class instance,
     * and because this constructor writes a static field.  Doing this
     * is bad in general, but it shuts up unwanted assurance failures for this
     * test.
     */
    lockT.lock();
    try {
      /* The immediately enclosing instance of s1 is "this" (a Test object)
       * There is no immediately enclosing instance with respect to Super.
       * 
       * Writes Super.count, this.t
       */
      final Super s1 = new Super() { 
        private int g = 10;
        { Test.this.t += 1; }
      };      
    } finally {
      lockT.unlock();
    }
  }

  @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT, this:T")
  public void stuff1() {
    /* The immediately enclosing instance of s2 is "this" (a Test object)
     * There is no immediately enclosing instance with respect to Super.
     * 
     * Writes Super.count, this.t
     */
    final Super s2 = new Super() { 
      private int g = 10;
      { t += 1; }
    };
  }

  public void stuff2() {
    /* The immediately enclosing instance of s2 is "this" (a Test object)
     * There is no immediately enclosing instance with respect to Super.
     * 
     * Writes Super.count, this.t
     */
    Super.lockCount.lock();
    try {
      this.lockT.lock();
      try {
        final Super s2 = new Super() { 
          private int g = 10;
          { t += 1; }
        };
      } finally {
        this.lockT.unlock();
      }
    } finally {
      Super.lockCount.unlock();
    }
  }

  @RegionLock("M1 is lockM1 protects m1")
  public class Middle1 {
    public final Lock lockM1 = new ReentrantLock();
    public int m1;
    
    @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT, Test.this:T")
    public Middle1() {
      /* I cannot declare this constructor to be singled threaded because
       * "this" is aliased by the creation of an anonymous class instance,
       * and because this constructor writes a static field.  Doing this
       * is bad in general, but it shuts up unwanted assurance failures for this
       * test.
       */
      lockM1.lock();
      try {
        /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
         * There is no immediately enclosing instance with respect to Super.
         * 
         * Writes Super.count, this.m1, Test.this.t
         */
        final Super s3 = new Super() { 
          private int g = 10;
          { m1 = 10; }
          { t += 1; }
        };
      } finally {
        lockM1.unlock();
      }
    }

    @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT, Test.this:T, this:M1")
    public void stuff1() {
      /* The immediately enclosing instance of s4 is "this" (a Middle1 object)
       * There is no immediately enclosing instance with respect to Super.
       * 
       * Writes Super.count, this.m1, Test.this.t
       */
      final Super s4 = new Super() { 
        private int g = 10;
        { m1 = 20; }
        { t += 1; }
      };
    }

    public void stuff2() {
      Super.lockCount.lock();
      try {
        Test.this.lockT.lock();
        try {
          lockM1.lock();
          try {
            /* The immediately enclosing instance of s4 is "this" (a Middle1 object)
             * There is no immediately enclosing instance with respect to Super.
             * 
             * Writes Super.count, this.m1, Test.this.t
             */
            final Super s4 = new Super() { 
              private int g = 10;
              { m1 = 20; }
              { t += 1; }
            };
          } finally {
            lockM1.unlock();
          }
        } finally {
          Test.this.lockT.unlock();
        }
      } finally {
        Super.lockCount.unlock();
      }
    }
  }
}
