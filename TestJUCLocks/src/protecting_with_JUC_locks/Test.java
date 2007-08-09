package protecting_with_JUC_locks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Borrowed;
import com.surelogic.RequiresLock;
import com.surelogic.ReturnsLock;
import com.surelogic.SingleThreaded;

@com.surelogic.Lock("L is lockField protects Instance")
public class Test {
  public final Lock lockField = new ReentrantLock();
  
  // protected for Test(int, int); unprotected for Test()
  private int x = 0;
  // protected for Test(int, int); unprotected for Test()
  private int y = 0;
  
  // Non-singlethreaded constructor
  public Test() {
    // not protected
    x = 1;
    // not protected
    y = 2;
  }

  @SingleThreaded
  @Borrowed("this")
  public Test(int a, int b) {
    // Holds L
    // protected
    x = a;
    // Holds L
    // protected
    y = b;
  }

  public void set(int a, int b) {
    lockField.lock();  // matches [a]
    try {
      // Holds L
      // protected
      x = a;
      // Holds L
      // protected
      y = b;
    } finally {
      // Holds L
      lockField.unlock();  // matches [a]
    }
  }
  
  public int getX() {
    // not protected
    return x;
  }
  
  @RequiresLock("L")
  public int getX2() {
    // Holds L
    // Protected
    return x;
  }
  
  @ReturnsLock("L")
  public Lock getLock() {
    return lockField;
  }
  
  public int getY() {
    lockField.lock(); // matches [b]
    try {
      // Holds L
      // protected
      return y;
    } finally {
      // Holds L
      lockField.unlock(); // matches [b]
    }
  }
}
