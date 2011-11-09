package test.AnonymousClass.SuperIsNotInner;

import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;

@RegionLock("T is lockT protects t")
public class Test {
  public final Object lockT = new Object();
  public int t;
  
  @RequiresLock("T")
  public void accessT() {}
  
  @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT")
  public Test() {
    /* I cannot declare this constructor to be singled threaded because
     * "this" is aliased by the creation of an anonymous class instance,
     * and because this constructor writes a static field.  Doing this
     * is bad in general, but it shuts up unwanted assurance failures for this
     * test.
     */
    synchronized (lockT) {
      /* The immediately enclosing instance of s1 is "this" (a Test object)
       * There is no immediately enclosing instance with respect to Super.
       * 
       * Writes Super.count, this.t
       */
      final Super s1 = new Super() { 
        private int g = 10;
        { Test.this.accessT(); }
      };
    }
  }

  @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT, this:T")
  public void stuff() {
    /* The immediately enclosing instance of s2 is "this" (a Test object)
     * There is no immediately enclosing instance with respect to Super.
     * 
     * Writes Super.count, this.t
     */
    final Super s2 = new Super() { 
      private int g = 10;
      { Test.this.accessT(); }
    };
  }

  @RegionLock("M1 is lockM1 protects m1")
  public class Middle1 {
    public final Object lockM1 = new Object();
    public int m1;
    
    @RequiresLock("M1")
    public void accessM1() {}
    
    @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT, Test.this:T")
    public Middle1() {
      /* I cannot declare this constructor to be singled threaded because
       * "this" is aliased by the creation of an anonymous class instance,
       * and because this constructor writes a static field.  Doing this
       * is bad in general, but it shuts up unwanted assurance failures for this
       * test.
       */
      synchronized (lockM1) {
        /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
         * There is no immediately enclosing instance with respect to Super.
         * 
         * Writes Super.count, this.m1, Test.this.t
         */
        final Super s3 = new Super() { 
          private int g = 10;
          { Middle1.this.accessM1(); }
          { Test.this.accessT(); }
        };
      }
    }

    @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT, Test.this:T, this:M1")
    public void stuff() {
      /* The immediately enclosing instance of s4 is "this" (a Middle1 object)
       * There is no immediately enclosing instance with respect to Super.
       * 
       * Writes Super.count, this.m1, Test.this.t
       */
      final Super s4 = new Super() { 
        private int g = 10;
        { Middle1.this.accessM1(); }
        { Test.this.accessT(); }
      };
    }
  }
}
