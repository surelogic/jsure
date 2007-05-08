/*
 * Created on Jan 24, 2005
 */
package test_static_lock_semantics.unrelated;

/**
 * This class declares static fields that are used as locks in other
 * non-descendent classes.
 * 
 * @lock L is Lock protects f
 */
public class C {
  // Used locally as a lock, and my the unrelated class D
  public static final Object Lock = new Object();
  public static final Object anotherLock = new Object();
  
//  @SuppressWarnings("unused")
  private static int f;
  
  public void good() {
    // GOOD
    synchronized (Lock) {
      f = 0;
    }
  }
}
