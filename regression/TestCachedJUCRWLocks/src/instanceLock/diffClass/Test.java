package instanceLock.diffClass;

public class Test {
  public void set(final C c, final int v) {
    c.wLock.lock();  // [a]
    try {
      c.data = v; // CORRECT
    } finally {
      c.wLock.unlock(); // [a]
    }
  }
  
  public int get(final C c) {
    c.rwLock.readLock().lock(); // [b]
    try {
      return c.data; // CORRECT
    } finally {
      c.rwLock.readLock().unlock(); // [b]
    }
  }
}
