package test.AnonymousClass.SuperIsNotInner;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;

@RegionLock("T is lockT protects t")
public class TestFieldInit {
  public final Lock lockT = new ReentrantLock();
  public int t;
  
  /* The immediately enclosing instance of s1 is "this" (a Test object)
   * There is no immediately enclosing instance with respect to Super.
   * 
   * Writes Super.count, this.t
   */
  final Super s1 = new Super() { 
    private int g = 10;
    { t += 1; } // Lock for T is NOT held
  };
  
  @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT")
  public TestFieldInit() {
    /* do nothing, but initializer for s1 is analyzed.  Effects from initializer
     * are writes Super.count, this.t.  Effect this.t is masked.
     */
    /* I cannot declare this constructor to be singled threaded because
     * "this" is aliased by the creation of an anonymous class instance above
     * and because this constructor writes a static field.  There is no way 
     * to surround the field initialization of "s1" above in a synchronized 
     * block.  It will have an assurance failure for field "t".
     */
  }



  @RegionLock("M1 is lockM1 protects m1")
  public class Middle1 {
    public final Lock lockM1 = new ReentrantLock();
    public int m1;
    
    /* The immediately enclosing instance of s3 is "this" (a Middle1 object)
     * There is no immediately enclosing instance with respect to Super.
     * 
     * Writes Super.count, this.m1, TestFieldInit.this.t
     */
    final Super s3 = new Super() { 
      private int g = 10;
      { m1 = 10; } // No way to hold m1
      { t += 1; }
    };      
    
    @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT, TestFieldInit.this:T")
    public Middle1() {
      /* Do nothing, but initializer for s3 is analyzed.  It has the effects
       * writes Super.count, this.m1, TestFieldInit.this.t.  The effect this.m1 
       * is masked.
       */
      /* I cannot declare this constructor to be singled threaded because
       * "this" is aliased by the creation of an anonymous class instance above
       * and because this constructor writes a static field.  There is no way 
       * to surround the field initialization of "s3" above in a synchronized 
       * block.  It will have an assurance failure for field "m1".
       */
    }

  
  
    @RegionLock("M2 is lockM2 protects m2")
    public class Middle2 {
      public final Lock lockM2 = new ReentrantLock();
      public int m2;

      /* The immediately enclosing instance of s5 is "this" (a Middle2 object)
       * There is no immediately enclosing instance with respect to Super.
       * 
       * Writes Super.count, this.m2, Middle1.this.m1, TestFieldInit.this.t
       */
      final Super s5 = new Super() { 
        private int g = 10;
        { m1 = 10; }
        { m2 = 20; } // No way to hold m2
        { t += 1; }
      };        
      
      @RequiresLock("test.AnonymousClass.SuperIsNotInner.Super:COUNT, TestFieldInit.this:T, test.AnonymousClass.SuperIsNotInner.TestFieldInit.Middle1.this:M1")
      public Middle2() {
        /* do nothing, but initializer for s5 is analyzed.  It has the effects
         * writes Super.count, this.m2, TestFieldInit.this.t, Middle1.this.m1.
         * The effect this.m2 is masked.
         */
        /* I cannot declare this constructor to be singled threaded because
         * "this" is aliased by the creation of an anonymous class instance above
         * and because this constructor writes a static field.  There is no way 
         * to surround the field initialization of "s5" above in a synchronized 
         * block.  It will have an assurance failure for field "m2".
         */
      }
    }
  }
}
