package test_static_lock_semantics.parent_child;

/**
 * Extends class A, uses static field from A as a lock.  Point here is that
 * "Lock", "A.Lock", and "B.LocK" all refer to the same field, so that each of 
 * the methods below acquires all three locks with one synchronized statement.
 * Once upon a time this didn't work correctly, but it should now.
 * 
 * @lock L1 is test_static_lock_semantics.parent_child.A:Lock protects x
 * @lock L2 is test_static_lock_semantics.parent_child.B:Lock protects y
 * @lock L3 is Lock protects z
 */
public class B extends A{
  private static int x;
  private static int y;
  private static int z;

  public static int good1() {
    // GOOD: Acquires L1, L2, L3
    synchronized(A.Lock) {
      return x + y + z;
    }
  }

  public static int good2() {
    // GOOD: Acquires L1, L2, L3
    synchronized(B.Lock) {
      return x + y + z;
    }
  }

  public static int good3() {
    // GOOD: Acquires L1, L2, L3
    synchronized(Lock) {
      return x + y + z;
    }
  }
}
