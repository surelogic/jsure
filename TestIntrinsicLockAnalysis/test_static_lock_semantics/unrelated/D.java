package test_static_lock_semantics.unrelated;
/*
 * Created on Jan 24, 2005
 */

/**
 * Uses static fields declared in another class as locks
 * 
 * @lock L1 is test_static_lock_semantics.unrelated.C:Lock protects g
 * @lock L2 is test_static_lock_semantics.unrelated.C:anotherLock protects h
 * @lock L3 is test_static_lock_semantics.unrelated.C:class protects i
 */
public class D {
  private static int g;
  private static int h;
  private static int i;
  
  public static int good1() {
    // GOOD
    synchronized(C.Lock) {
      return g;
    }
  }
  
  public static int good2() {
    // GOOD
    synchronized(C.anotherLock) {
      return h;
    }
  }
  
  public static int good3() {
    // GOOD
    synchronized(C.class) {
      return i;
    }
  }
}
