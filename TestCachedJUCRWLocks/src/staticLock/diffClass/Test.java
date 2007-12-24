package staticLock.diffClass;

public class Test {
  public static void set(final int v) {
    C.wLock.lock(); // [a]
    try {
      C.data = v; // GOOD
    } finally {
      C.wLock.unlock(); // [a]
    }
  }
  
  public static int get() {
    C.rwLock.readLock().lock(); // [b]
    try {
      return C.data; // GOOD
    } finally {
      C.rwLock.readLock().unlock(); // [b]
    }
  }
}
