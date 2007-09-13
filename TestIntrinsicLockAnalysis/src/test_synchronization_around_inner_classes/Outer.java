package test_synchronization_around_inner_classes;

import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;

/**
 * Test file to see if the scope of a synchronized block incorrectly
 * captures nested anonymous classes declared within it.
 */
@Regions({
  @Region("private static StaticRegion"),
  @Region("private InstanceRegion")
})
@RegionLocks({
  @RegionLock("StaticLock is staticLock protects StaticRegion"),
  @RegionLock("InstanceLock is this protects InstanceRegion")
})
public class Outer {
  private static final Object staticLock = new Object();

  @InRegion("StaticRegion")
  private static int staticField;

  @InRegion("InstanceRegion")
  private int instanceField;
  
  public void foo() {
    synchronized (staticLock) {
      // GOOD
      staticField = 0; 
      // GOOD
      staticField = 1;
    }
    
    synchronized(this) {
      // GOOD
      instanceField = 5;
    }
  }

  public static void okayStatic_implicitClass() {
    synchronized (staticLock) {
      // GOOD
//      @SuppressWarnings("unused")
      int x = staticField;
      
//      @SuppressWarnings("unused")
      Runnable o = new Runnable() {
        public void run() {
          synchronized(staticLock) {
            // GOOD
            staticField = 3;
          }
        }
      };
    }
  }

  public static void okayStatic2_explicitClass() {
    synchronized (staticLock) {
      // GOOD
//      @SuppressWarnings("unused")
      int x = staticField;
      
//      @SuppressWarnings("unused")
      Runnable o = new Runnable() {
        public void run() {
          synchronized(Outer.staticLock) {
            // GOOD
            staticField = 3;
          }
        }
      };
    }
  }

  public static void badStatic() {
    synchronized (staticLock) {
      // GOOD
//      @SuppressWarnings("unused")
      int x = staticField;
      
//      @SuppressWarnings("unused")
      Runnable o = new Runnable() {
        public void run() {
          // BAD: not protected, but a badly written analysis might think it is
          staticField = 3;
        }
      };
    }
  }

  public void okayInstance() {
    synchronized (this) {
      // GOOD
//      @SuppressWarnings("unused")
      int x = instanceField;
      
//      @SuppressWarnings("unused")
      Runnable o = new Runnable() {
        public void run() {
          synchronized(Outer.this) {
            // GOOD
            // XXX: BUG 611 keeps this from working correctly
            instanceField = 3;
          }
        }
      };
    }
  }

  public void okayInstance2() {
    synchronized (this) {
      // GOOD
//      @SuppressWarnings("unused")
      int x = instanceField;
      
//      @SuppressWarnings("unused")
      Runnable o = new Runnable() {
        public void run() {
          synchronized(Outer.this) {
            // GOOD
            Outer.this. instanceField = 3;
          }
        }
      };
    }
  }

  public void badInstance() {
    synchronized (this) {
      // GOOD
//      @SuppressWarnings("unused")
      int x = instanceField;
      
//      @SuppressWarnings("unused")
      Runnable o = new Runnable() {
        public void run() {
          // BAD: not protected, but a badly written analysis might think it is
          instanceField = 3;
        }
      };
    }
  }

  public void badInstance2() {
    synchronized (this) {
      // GOOD
//      @SuppressWarnings("unused")
      int x = instanceField;
      
//      @SuppressWarnings("unused")
      Runnable o = new Runnable() {
        public void run() {
          // BAD: not protected, but a badly written analysis might think it is
          Outer.this. instanceField = 3;
        }
      };
    }
  }
}
